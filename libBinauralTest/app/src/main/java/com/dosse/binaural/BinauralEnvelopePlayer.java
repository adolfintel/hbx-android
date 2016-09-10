/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dosse.binaural;

import java.io.ObjectInputStream;
import java.util.zip.GZIPInputStream;

import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import com.dosse.binaural.BinauralEnvelope.Envelope;

/**
 * use this class to play a BinauralEnvelope to the speaker. sample format:
 * 44100Hz, 16bit, stereo, little endian. BinauralEnvelope's time unit is
 * supposed to be seconds. USE HEADPHONES!
 * 
 * @author dosse
 */
public class BinauralEnvelopePlayer extends Thread {

	private Envelope binaural, noise, binauralV;
	private AudioTrack speaker;
	private double baseF = 220;
	private double volume = 1;

	public BinauralEnvelopePlayer(BinauralEnvelope be) {
		this.binaural = be.getBinauralF();
		this.binauralV = be.getBinauralV();
		this.noise = be.getNoiseV();
		this.baseF = be.getBaseF();
		int buff=AudioTrack.getMinBufferSize(
				44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT);
		if(buff<32768){
			Log.v("HBX-BUFFER", "Android suggests a ridiculusly small sound buffer of "+buff+" samples; using 32768 instead");
			buff=32768;
		}
		speaker = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT, buff, AudioTrack.MODE_STREAM);
		speaker.play();
	}

	private double t = 0; // time
	private double ctL = 0, ctR = 0; // used for internal calculations
	// NOISE SAMPLE STUFF START
	private static int noiseSampleLen = 1;
	private static double[] noiseSample = new double[noiseSampleLen];
	private static int ns = 0;
	private static boolean noiseReady = false;

	public static void loadNoiseFromAssets(final AssetManager as) {
		new Thread() {
			public void run() {
				try {
					ObjectInputStream ois = new ObjectInputStream(
							new GZIPInputStream(as.open("noise.dat")));
					short[] noiseSample16Bit = (short[]) ois.readObject();
					noiseSampleLen = noiseSample16Bit.length;
					noiseSample = new double[noiseSampleLen];
					for (int i = 0; i < noiseSampleLen; i++) {
						noiseSample[i] = 1.4 * (((double) noiseSample16Bit[i]) / ((double) Short.MAX_VALUE)); // 1.4
																												// is
																												// preamp
					}
				} catch (Throwable t) {
					System.err.println(t);
					noiseSampleLen = 1;
					noiseSample = new double[noiseSampleLen];
				}
				noiseReady = true;
			}
		}.start();
	}

	/**
	 * 
	 * @return next sample of noise as double
	 */
	private static double nextNoiseSample() {
		return noiseSample[ns++ % noiseSampleLen];
	}

	// NOISE SAMPLE STUFF END

	/**
	 * 
	 * @return current position (in seconds)
	 */
	public double getT() {
		return t;
	}

	/**
	 * 
	 * @param t
	 *            new position (in seconds)
	 */
	public void setT(double t) {
		this.t = t;
	}

	/**
	 * 
	 * @return length in seconds of the BinauralEnvelope (in seconds)
	 */
	public double getLength() {
		return binaural.getLength();
	}

	/**
	 * 
	 * @return start time of the BinauralEnvelope (in seconds)
	 */
	public double getStartT() {
		return binaural.getStartT();
	}

	/**
	 * 
	 * @return end time of the BinauralEnvelope (in seconds)
	 */
	public double getEndT() {
		return binaural.getEndT();
	}

	/**
	 * 
	 * @return position in BinauralEnvelope as double 0-1. may be below 0 or
	 *         above 1 if the position is before getStartT or after getEndT.
	 */
	public double getPosition() {
		return (getT() - getStartT()) / (getEndT() - getStartT());
	}

	/**
	 * sets the new position in the envelope as double 0-1
	 * 
	 * @param p
	 *            the new position as double 0-1 (you may use values below 0 or
	 *            above 1, but it's not recommended)
	 */
	public void setPosition(double p) {
		t = getStartT() + (getEndT() - getStartT()) * p;
		if (p == 0) {
			ctL = 0;
			ctR = 0;
		} // reset ct if needed
	}

	/**
	 * 
	 * @return volume as double 0-1
	 */
	public double getVolume() {
		return volume;
	}

	/**
	 * 
	 * @param volume
	 *            volume as double 0-1. if lower than 0 or higher than 1, an
	 *            IllegalArgumentException is thrown
	 */
	public void setVolume(double volume) {
		if (volume < 0 || volume > 1) {
			throw new IllegalArgumentException("Volume must be a double 0-1");
		} else {
			this.volume = volume;
		}
	}

	private boolean killASAP = false;

	/**
	 * stops playback and kills this thread
	 */
	public void stopPlaying() {
		killASAP = true;
	}

	/**
	 * set to true to pause playback. set to false to unpause playback. default
	 * value is false, of course
	 */
	public boolean paused = false;

	// SINE LUT STUFF START
	private final static int LUT_SIZE = 8192;
	private final static double[] LUT = new double[LUT_SIZE];
	private final static double STEP_SIZE = (2 * Math.PI) / LUT_SIZE;
	private static boolean sineReady = false;

	static {
		new Thread() {
			public void run() {// init LUT
				for (int i = 0; i < LUT_SIZE; i++) {
					LUT[i] = Math.sin(STEP_SIZE * i);
				}
				sineReady = true;
			}
		}.start();
	}

	private final static double PI2 = Math.PI * 2;

	/**
	 * fast approximate sine (with LUT table). note: do not use with high
	 * frequencies as it tends to produce garbage
	 * 
	 * @param x
	 *            angle in rad
	 * @return sin(x)
	 */
	public final static double fastSin(double x) {
		x %= PI2;
		x = x >= 0 ? x : (PI2 + x);
		return LUT[(int) (x / STEP_SIZE)]; // nearest (no interpolation due to
											// incredible speed of ARM+dalvik
											// combo)
	}

	// SINE LUT STUFF END
	private int playbackProblems = 0; // increased by 5 at each buffer underrun,
										// or by 10 if the underrun is very long
										// (at least twice as much as it takes
										// to play the buffer); decreased by 1
										// at each buffer delivered in time. min
										// is 0

	public int getPlaybackProblems() {
		return playbackProblems;
	}

	public void resetPlaybackProblems() {
		playbackProblems = 0;
	}

	@Override
	public void run() {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);
		t = getStartT();
		final double tStep = 1.0 / 44100.0;
		final int buffLen = 4096;
		final long bufferLenNanoSeconds = (long) ((buffLen / 44100.0) * 1000000000L); // used
		// to
		// detect
		// buffer
		// underruns
		final long bufferLenNanoSeconds2 = bufferLenNanoSeconds * 2; // used to
																		// detect
		// long buffer
		// underruns
		final short[] toSoundCard = new short[buffLen * 2];
		while (!sineReady || !noiseReady) {
			try {
				sleep(10);
			} catch (InterruptedException e) {
			}
		}
		for (;;) {
			if (killASAP) {
				speaker.stop();
				speaker.release();
				return;
			}
			if (paused) {
				speaker.stop();
				while (paused) {
					try {
						sleep(100);
					} catch (InterruptedException e) {
					}
				}
				int buff=AudioTrack.getMinBufferSize(
						44100, AudioFormat.CHANNEL_CONFIGURATION_STEREO,
						AudioFormat.ENCODING_PCM_16BIT);
				if(buff<32768){
					Log.v("HBX-BUFFER", "Android suggests a ridiculusly small sound buffer of "+buff+" samples; using 32768 instead");
					buff=32768;
				}
				speaker = new AudioTrack(AudioManager.STREAM_MUSIC, 44100,
						AudioFormat.CHANNEL_CONFIGURATION_STEREO,
						AudioFormat.ENCODING_PCM_16BIT, buff, AudioTrack.MODE_STREAM);
				speaker.play();
			}
			final long startT = System.nanoTime(); // used to detect buffer
													// underruns
			final double volumeMul = volume * Short.MAX_VALUE;
			final double binauralVolume = binauralV.getValueAt(t) * 0.55;
			final double noiseVolume = noise.getValueAt(t);
			double frequencyShift = binaural.getValueAt(t) * 0.5;
			if (Double.isNaN(frequencyShift))
				frequencyShift = 0;
			for (int i = 0; i < buffLen; i++) {
				final double pinkNoise = nextNoiseSample() * noiseVolume;
				final double ld = binauralVolume * fastSin(PI2 * ctL)
						+ pinkNoise;
				final double rd = binauralVolume * fastSin(PI2 * ctR)
						+ pinkNoise;
				toSoundCard[2 * i] = (short) (volumeMul * ld);
				toSoundCard[2 * i + 1] = (short) (volumeMul * rd);
				t += tStep;
				ctL += (baseF - frequencyShift) / 44100.0;
				ctR += (baseF + frequencyShift) / 44100.0;
			}
			final long diff = System.nanoTime() - startT;
			if (diff >= bufferLenNanoSeconds) { // buffer underrun
				if (diff >= bufferLenNanoSeconds2)
					playbackProblems += 10; // long buffer underrun
				else
					playbackProblems += 5;
			} else if (playbackProblems > 0)
				playbackProblems--; // buffer delivered in time
			speaker.write(toSoundCard, 0, toSoundCard.length);
		}
	}
}

// Conditions of use: You are free to use this software for research or
// educational purposes. In addition, we expect you to include adequate
// citations and acknowledgments whenever you present or publish results that
// are based on it.
//% *********************************************************************************
//% It is a part of publication:
//% Weisong Zhao et al. Quantitatively mapping local quality of
//% super-resolution microscopy by rolling Fourier ring correlation,
//% Light Science & Applications (2023).
//% *********************************************************************************
//%    Copyright 2019~2023 Weisong Zhao et al.
//%
//%    This program is free software: you can redistribute it and/or modify
//%    it under the terms of the Open Data Commons Open Database License v1.0.
//%
//%    This program is distributed in the hope that it will be useful,
//%    but WITHOUT ANY WARRANTY; without even the implied warranty of
//%    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//%    Open Data Commons Open Database License for more details.
//%
//%    You should have received a copy of the
//%    Open Data Commons Open Database License
//%    along with this program.  If not, see:
//%    <https://opendatacommons.org/licenses/odbl/>.

package com.HIT.weisongzhao;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JDialog;

import com.HIT.weisongzhao.utils.GrayLevelClass;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

public class PANELJ_ extends JDialog implements PlugIn {
	private static int blockSize = 64;
	private static int backgroundIntensity = 15;
	private static int skip = 1;
	private static float pixelSize = 20;

	@Override
	public void run(String arg) {

		if (IJ.versionLessThan("1.46j"))
			return;
		int[] wList = WindowManager.getIDList();
		if (wList == null) {
			IJ.noImage();
			return;
		}
		String[] titles = new String[wList.length];
		for (int i = 0; i < wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (imp != null)
				titles[i] = imp.getTitle();
			else
				titles[i] = "";
		}

		String titleImage = Prefs.get("PANEL.titleImage", titles[0]);
		int imageChoice = 0;
		for (int i = 0; i < wList.length; i++) {
			if (titleImage.equals(titles[i])) {
				imageChoice = i;
				break;
			}
		}

		GenericDialog gd = new GenericDialog("rFRC - 3-sigma curve");
		gd.addChoice("Image sequence", titles, titles[imageChoice]);
		gd.addNumericField("Block Size", blockSize, 0, 5, "pixel (64 in default)");
		gd.addNumericField("Background Intensity", backgroundIntensity, 0, 5, "0~255 (background of your data)");
		gd.addNumericField("Skip", skip, 0, 3, "pixel (Speed up calculation, 1~block-size/2)");
		gd.addNumericField("Pixel Size", pixelSize, 2, 5, "(nm)");
		gd.addHelp("https://github.com/WeisongZhao/PANELJ");
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		// Get parameters
		blockSize = (int) gd.getNextNumber();
		backgroundIntensity = (int) gd.getNextNumber();
		skip = (int) gd.getNextNumber();
		pixelSize = (float) gd.getNextNumber();
		ImagePlus impY = WindowManager.getImage(wList[gd.getNextChoiceIndex()]);

		if (!showDialog())
			return;

		rFRC(impY, blockSize, backgroundIntensity, skip, pixelSize);

	}

	private boolean showDialog() {

		return true;
	}

	private void rFRC(ImagePlus stack, int blockSize, int backgroundIntensity, int skip, float pixelSize) {
		if (blockSize % 2 != 0)
			blockSize = blockSize + 1;
		skip = min(skip, blockSize / 2);
		FRC myFRC = new FRC();
		int w = stack.getWidth();
		int h = stack.getHeight();
		int l = stack.getImageStackSize();
		int padw = w + blockSize;
		int padh = h + blockSize;
		ImageStack ims = stack.getStack();
		ImageStack result = new ImageStack(w, h);
		ImageStack resultmask = new ImageStack(w, h);
		ResultsTable rt = new ResultsTable();
		for (int t = 1; t < l; t++) {
			ImageProcessor ip1s = ims.getProcessor(t);
			ImageProcessor ip2s = ims.getProcessor(t + 1);
			FloatProcessor ip1 = createPaddedImage(ip1s, padw, padh).convertToFloatProcessor();
			FloatProcessor ip2 = createPaddedImage(ip2s, padw, padh).convertToFloatProcessor();
			double max1d = ip1.getMax();
			double max2d = ip2.getMax();
			float max1 = (float) max1d;
			float max2 = (float) max2d;
			float value1;
			float value2;
			float BI = backgroundIntensity;
			double resolution;
			ArrayList<double[]> vectors = new ArrayList<double[]>();

			for (int xStart = 0; xStart < h; xStart += skip) {
				for (int yStart = 0; yStart < w; yStart += skip) {
					IJ.showStatus("Start rFRC mapping - 3-sigma curve");
					IJ.showProgress(w * xStart + yStart, w * h);
					FloatProcessor ipROI1 = getROI(ip1, yStart, xStart, blockSize, blockSize);
					FloatProcessor ipROI2 = getROI(ip2, yStart, xStart, blockSize, blockSize);
					float suming = 0;
					int flage = 0;
					for (int xsum = 0; xsum < skip; xsum++) {
						for (int ysum = 0; ysum < skip; ysum++) {
							value1 = ipROI1.getf(blockSize / 2 - 1 + xsum, blockSize / 2 - 1 + ysum);
							value2 = ipROI2.getf(blockSize / 2 - 1 + xsum, blockSize / 2 - 1 + ysum);
							suming = suming + value1 / max1 + value2 / max2;
							flage = flage + 1;
						}
					}
					suming = suming / flage / 2;
					if ((suming * 255) > (BI)) {

						resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, FRC.ThresholdMethod.THREE_SIGMA);
						if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
							vectors.add(new double[] { yStart, xStart, resolution * pixelSize });
						} else {
							resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, FRC.ThresholdMethod.FIVE_SIGMA)
									* 3 / 5;
							if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
								vectors.add(new double[] { yStart, xStart, resolution * pixelSize });
							} else {
//								resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, FRC.ThresholdMethod.FIXED_1_OVER_7)
//										* 1.143;
								resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, FRC.ThresholdMethod.TWO_SIGMA)
										* 3 / 2;
								if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
									vectors.add(new double[] { yStart, xStart, resolution * pixelSize });
								} 
							}
						}

					}
				}
			}
			IJ.showStatus("Ensemble results");
			int nValues = vectors.size();
			float[] xPositions = new float[nValues];
			float[] yPositions = new float[nValues];
			float[] values = new float[nValues];
			int counter = 0;
			double mean = 0, max = -Double.MAX_VALUE, min = Double.MAX_VALUE;
			for (double[] vector : vectors) {
				xPositions[counter] = (int) vector[0];
				yPositions[counter] = (int) vector[1];
				values[counter] = (float) vector[2];
				min = Math.min(values[counter], min);
				counter++;
			}

			float[] rFRCMAP = new float[w * h];
			float[] rFRCMASK = new float[w * h];

			float position;
			int positionint;
			for (int p = 0; p < nValues; p++) {
				for (int xx = 0; xx < skip; xx++) {
					for (int yy = 0; yy < skip; yy++) {
						position = (min((yPositions[p] + yy), h - 1) * w + min((xPositions[p] + xx), w - 1));
						positionint = min(max(min(round(position), Integer.MAX_VALUE), 0), w * h - 1);
						rFRCMAP[positionint] = values[p];
						rFRCMAP[Math.min(positionint + 1, w * h - 1)] = values[p];
						rFRCMASK[positionint] = max(values[p] / (float) min - (float) 1.4, 0);
						rFRCMASK[Math.min(positionint + 1, w * h - 1)] = max(values[p] / (float) min - (float) 1.4, 0);
					}
				}
			}
//			rFRCMAP = AMF(rFRCMAP, w, h);
			if (skip == 1) {rFRCMAP = AMF(rFRCMAP, w, h);}
			counter = 0;
			for (int pixel = 0; pixel < w * h; pixel++) {
				if (rFRCMAP[pixel] == 0)
					;
				else {
					mean += rFRCMAP[pixel];
					counter++;
					max = Math.max(rFRCMAP[pixel], max);
					min = Math.min(rFRCMAP[pixel], min);
				}
			}

			FRCData data = new FRCData();
			data.meanFRC = mean /= counter;
			data.maxFRC = max;
			data.minFRC = min;
			data.rFRC = mean / min - 1;
			rt.incrementCounter();
			rt.addValue("Mean FRC (nm)", data.meanFRC);
			rt.addValue("rFRC value", data.rFRC);
			rt.addValue("Min FRC (nm)", data.minFRC);
			rt.addValue("Max FRC (nm)", data.maxFRC);
			if (skip == 1) {rFRCMAP = AMF(rFRCMAP, w, h);}
//			rFRCMASK = AMF(rFRCMASK, w, h);
			result.addSlice("", rFRCMAP);
			resultmask.addSlice("", rFRCMASK);
		}

		ImagePlus image = new ImagePlus("rFRC Map", result);
		ImagePlus mask = new ImagePlus("", resultmask);
		ImageConverter ic = new ImageConverter(mask);
		ic.convertToGray8();
		ImageStack msslice = new ImageStack(w, h);
		ImageStack msstack = new ImageStack(w, h);
		ImageProcessor ms;
		for (int slice = 0; slice < mask.getImageStackSize(); slice++) {
			ms = mask.getStack().getProcessor(slice + 1);
			msslice = ostu(ms);
			msstack.addSlice("", msslice.getProcessor(1));
		}

		NJ_LUT.applyLUT_PANEL_rFRC(image);
		image.show();
		ImagePlus maskshow = new ImagePlus("Simplified PANEL (without RSM)", msstack);
		NJ_LUT.applyLUT_PANEL_rFRCmask(maskshow);
		ij.IJ.run("Calibration Bar...",
				"location=[Lower Right] fill=Black label=White number=4 decimal=0 font=12 zoom=1 bold");
		maskshow.show();
		rt.show("rFRC-Mapping metrics - 3-sigma curve");

	}

	private float[] AMF(float[] fp, int w, int h) {

		int width = w;
		int height = h;
		int window = 3;
		float threshold = 2;
		float[] pixels = fp;
		float[] temp = new float[window * window];
		float[] newpix = pixels;
		int win = (3 - 1) / 2;

		for (int y = win; y < height - win; y++) {
			for (int x = win; x < width - win; x++) {
				int flage = 0;

				for (int i = -win; i <= win; i++) {
					for (int j = -win; j <= win; j++) {
						temp[flage] = newpix[((x + i + (y + j) * width))];
						flage = flage + 1;
					}
				}
				Arrays.sort(temp);
				if (threshold * temp[(flage) / 2] < newpix[(x + y * width)]) {
					pixels[(x + y * width)] = temp[flage / 2];
				}
			}
		}
		return pixels;
	}

	private ImageStack ostu(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();

		GrayLevelClass.N = width * height;
		GrayLevelClass.probabilityHistogramDone = false;
		GrayLevelClass C1 = new GrayLevelClass((ByteProcessor) ip, true);
		GrayLevelClass C2 = new GrayLevelClass((ByteProcessor) ip, false);

		float fullMu = C1.getOmega() * C1.getMu() + C2.getOmega() * C2.getMu();

		double sigmaMax = 0;
		int threshold = 0;

		/** Start **/
		for (int i = 0; i < 255; i++) {

			double sigma = C1.getOmega() * (Math.pow(C1.getMu() - fullMu, 2))
					+ C2.getOmega() * (Math.pow(C2.getMu() - fullMu, 2));

			if (sigma > sigmaMax) {
				sigmaMax = sigma;
				threshold = C1.getThreshold();
			}

			C1.addToEnd();
			C2.removeFromBeginning();
		}

//		ImagePlus imp = NewImage.createByteImage("Threshold", width, height, 1, NewImage.FILL_WHITE);
		ImagePlus imp = new ImagePlus("", ip);
		ImageProcessor nip = imp.getProcessor();

		byte[] pixels = (byte[]) ((ByteProcessor) ip).getPixels();

		int offset = 0;

		for (int y = 0; y < height; y++) {
			offset = y * width;
			for (int x = 0; x < width; x++) {
				if ((pixels[offset + x] & 0xff) <= threshold)
					nip.putPixel(x, y, 0);
			}
		}
//		IJ.showMessage("Found threshold : "+threshold);
		return imp.getStack();
	}

	private FloatProcessor getROI(FloatProcessor ip, int x, int y, int w, int h) {
		FloatProcessor ipCrop = new FloatProcessor(w, h);
//		float ipmax = (float) ip.getMax();
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {

				ipCrop.setf(i, j, ip.getf(x + i, y + j));
			}
		}
		return ipCrop;
	}

	class FRCData {
		public FloatProcessor FRCMap;
		public FloatProcessor FRCMask;
		public double meanFRC;
		public double stdDevFRC;
		public double minFRC;
		public double maxFRC;
		public double rFRC;
		public int nValues;
	}

	/**
	 * createPaddedImage creates an image with added border to decrease border
	 * problematic with filters.
	 * 
	 * @param ip           - original image processor
	 * @param paddedWidth  - The final padded image width
	 * @param paddedHeight - The final padded image height
	 * @param paddedOption - 0 , 1 - asymmetric or symmetric
	 * @return padded imageProcessor
	 */
	private static ImageProcessor createPaddedImage(ImageProcessor ip, int paddedWidth, int paddedHeight) {
		ImageProcessor paddedProcessor;
		int imageWidth = ip.getWidth();
		int extraWidth = paddedWidth - imageWidth;
		int imageHeight = ip.getHeight();
		int extraHeight = paddedHeight - imageHeight;
//		System.out.printf("%d",paddedWidth);
		// ImagePlus paddedImg;//For debugging -> possible to show padded image.

		// Cast image in 8bits/16bits in 32 bits images.
		float[] pixArray = (float[]) ip.convertToFloatProcessor().getPixels();
		paddedProcessor = new FloatProcessor(paddedWidth, paddedHeight);
		float[] paddedArray = new float[paddedWidth * paddedHeight];
		// Start by copying the interior of the image + first padded rows and columns.
		for (int row = 0; row < imageHeight; row++) {
			int imageOffset = row * imageWidth;
			int paddedOffset = row * paddedWidth + ((extraHeight / 2) * paddedWidth);

			for (int col = 0; col < imageWidth; col++) {
				// COPY INTERIOR
				paddedArray[col + paddedOffset + extraWidth / 2] = pixArray[col + imageOffset];
			}
		}

		// Fill top and bottom border
		for (int row = 0; row < extraHeight / 2; row++) {
			// Calculate the different index for the different offSets
			int topOffset = paddedWidth * (extraHeight / 2 - 1 - row);
			int firstTopElementOffset = (extraHeight / 2 + row) * paddedWidth;
			int secondTopElementOffset = firstTopElementOffset + paddedWidth;

			int botOffset = paddedWidth * (paddedHeight - extraHeight / 2 + row);
			int firstBotElementOffset = paddedWidth * (paddedHeight - extraHeight / 2 - 1 - row);
			int secondBotElementOffset = firstBotElementOffset - paddedWidth;

			int colOffset = (extraWidth / 2) - row;

			for (int col = 0; col < paddedWidth - (extraWidth / 2 - row); col++) {

				// FILL TOP
				int firstTopIdx = col + colOffset + firstTopElementOffset;
				int secondTopIdx = col + colOffset + secondTopElementOffset;
				int topIdx = col + colOffset + topOffset;
				float diff = paddedArray[secondTopIdx] - paddedArray[firstTopIdx];
				float value = 0;

				value = (paddedArray[topIdx + paddedWidth]) + diff;
				paddedArray[topIdx] = value;

				// FILL BOT
				int firstBotIdx = col + colOffset + firstBotElementOffset;
				int secondBotIdx = col + colOffset + secondBotElementOffset;
				int botIdx = col + colOffset + botOffset;

				diff = paddedArray[secondBotIdx] - paddedArray[firstBotIdx];

				value = paddedArray[botIdx - paddedWidth] + diff;
				paddedArray[botIdx] = value;

			}

		}

		// Fill left and right
		for (int col = 0; col < extraWidth / 2; col++) {
			int colOffset = extraWidth / 2 - col;
			int rightColOffset = paddedWidth - 1 - extraWidth / 2 + col;
			for (int row = 0; row < paddedHeight; row++) {
				int rowOffset = row * paddedWidth;

				int currentLeftValueIdx = rowOffset + colOffset;
				int leftIdx = currentLeftValueIdx - 1;
				int firstLeftElementIdx = rowOffset + colOffset + col * 2;
				int secondLeftElementIdx = firstLeftElementIdx + 1;

				int currentRightValueIdx = rowOffset + rightColOffset;
				int rightIdx = currentRightValueIdx + 1;
				int firstRightElementIdx = rowOffset + rightColOffset - col * 2;
				int secondRightElementIdx = firstRightElementIdx - 1;

				float value = 0;
				// LEFT
				float diff = paddedArray[secondLeftElementIdx] - paddedArray[firstLeftElementIdx];

				value = paddedArray[currentLeftValueIdx] + diff;
				paddedArray[leftIdx] = value;
				// RIGHT
				diff = paddedArray[secondRightElementIdx] - paddedArray[firstRightElementIdx];

				value = paddedArray[currentRightValueIdx] + diff;
				paddedArray[rightIdx] = value;
			}
		}
		paddedProcessor.setPixels(paddedArray);
		return paddedProcessor;
		// paddedImg = new ImagePlus("Background Image",paddedProcessor);
		// paddedImg.show();
	}

	public static void main(String[] args) {

		Class<?> clazz = PANELJ_.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(),
				url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);
		new ImageJ();
		ImagePlus image = IJ.openImage();
//		ImagePlus psf = IJ.openImage();
		image.show();
//		psf.show();
		IJ.runPlugIn(clazz.getName(), "");
	}

}
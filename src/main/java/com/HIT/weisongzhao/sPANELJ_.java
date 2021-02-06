/* 
* Conditions of use: You are free to use this software for research or
* educational purposes. In addition, we expect you to include adequate
* citations and acknowledgments whenever you present or publish results that
* are based on it.
* 
* Reference: [1]. Weisong Zhao, et al. "PANEL (2021).
*/

/*
 * Copyright 2020 Weisong Zhao.
 * 
 * This file is part of PANEL Analyze plugin (PANEL).
 * 
 * PANEL is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * PANEL is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * PANEL. If not, see <http://www.gnu.org/licenses/>.
 */

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

public class sPANELJ_ extends JDialog implements PlugIn {
	private static int blockSize = 32;
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

		GenericDialog gd = new GenericDialog("Single-frame rFRC (3-sigma curve)");
		gd.addChoice("Image sequence", titles, titles[imageChoice]);
		gd.addNumericField("Block Size", blockSize, 0, 5, " pixel ");
		gd.addNumericField("Background Intensity", backgroundIntensity, 0, 5, "0~255");
		gd.addNumericField("Skip", skip, 0, 3, "pixel (Speed up calculation)");
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

		single_rFRC(impY, blockSize, backgroundIntensity, skip, pixelSize);

	}

	private boolean showDialog() {

		return true;
	}

	private void single_rFRC(ImagePlus stack, int blockSize, int backgroundIntensity, int skip, float pixelSize) {
		if (blockSize % 2 != 0)
			blockSize = blockSize + 1;

		skip = min(skip, blockSize / 2);
		int w = stack.getWidth();
		int h = stack.getHeight();
		int l = stack.getImageStackSize();
		int wsub = w;
		int hsub = h;
		if (w % 2 != 0)
			wsub = w-1;
		if (h % 2 != 0)
			hsub = h-1;

		ImageStack ims = stack.getStack();
		ImageStack result = new ImageStack(w / 2, h / 2);
		ImageStack resultmask = new ImageStack(w / 2, h / 2);
		ResultsTable rt = new ResultsTable();
		float[] pixsub1 = new float[w * h / 4];
		float[] pixsub2 = new float[w * h / 4];
		float[] pixsub3 = new float[w * h / 4];
		float[] pixsub4 = new float[w * h / 4];
		ImageProcessor ips1 = new FloatProcessor(w / 2, h / 2);
		ImageProcessor ips2 = new FloatProcessor(w / 2, h / 2);
		ImageProcessor ips3 = new FloatProcessor(w / 2, h / 2);
		ImageProcessor ips4 = new FloatProcessor(w / 2, h / 2);
		ArrayList<double[]> vectors1 = new ArrayList<double[]>();
		ArrayList<double[]> vectors2 = new ArrayList<double[]>();

		for (int t = 1; t < l+1 ; t++) {
			ImageProcessor ip = ims.getProcessor(t);
			float[] pixArray = (float[]) ip.convertToFloatProcessor().getPixels();
			
			for (int x = 0; x < hsub; x = x + 2) {
//				System.out.printf("%d-", x);
				for (int y = 0; y < wsub; y = y + 2) {

					pixsub1[x / 2 * (w / 2) + y / 2] = pixArray[x * wsub + y];
					pixsub2[x / 2 * (w / 2) + y / 2] = pixArray[(x + 1) * wsub + y];
					pixsub3[x / 2 * (w / 2) + y / 2] = pixArray[x * wsub + y + 1];
					pixsub4[x / 2 * (w / 2) + y / 2] = pixArray[(x + 1) * wsub + y + 1];
				}
			}

			ips1.setPixels(pixsub1);
			ips2.setPixels(pixsub2);
			ips3.setPixels(pixsub3);
			ips4.setPixels(pixsub4);
			
			
			vectors1 = rFRC(ips1, ips4, blockSize, backgroundIntensity, skip, pixelSize);
			vectors2 = rFRC(ips3, ips2, blockSize, backgroundIntensity, skip, pixelSize);
			IJ.showStatus("Ensemble results");
			int nValues1 = vectors1.size();
			int nValues2 = vectors2.size();
			float[] xPositions1 = new float[nValues1];
			float[] yPositions1 = new float[nValues1];
			float[] values1 = new float[nValues1];
			float[] xPositions2 = new float[nValues2];
			float[] yPositions2 = new float[nValues2];
			float[] values2 = new float[nValues2];
			int counter = 0;
			double mean = 0, max = -Double.MAX_VALUE, min = Double.MAX_VALUE;
			for (double[] vector : vectors1) {
				xPositions1[counter] = (int) vector[0];
				yPositions1[counter] = (int) vector[1];
				values1[counter] = (float) vector[2];

				counter++;
			}
			counter = 0;
			for (double[] vector : vectors2) {
				xPositions2[counter] = (int) vector[0];
				yPositions2[counter] = (int) vector[1];
				values2[counter] = (float) vector[2];

				counter++;
			}

			float[] rFRCMAP = new float[w * h / 4];
			float[] rFRCMAP2 = new float[w * h / 4];
			float[] rFRCMASK = new float[w * h];

			float position;
			int positionint;
			for (int p = 0; p < nValues1; p++) {
				for (int xx = 0; xx < skip; xx++) {
					for (int yy = 0; yy < skip; yy++) {
						position = (min((yPositions1[p] + yy), (h / 2) - 1) * (w / 2)
								+ min((xPositions1[p] + xx), (w / 2) - 1));
						positionint = min(max(min(round(position), Integer.MAX_VALUE), 0), w * h / 4 - 1);
						rFRCMAP[positionint] = values1[p];
					}
				}
			}

			for (int p = 0; p < nValues2; p++) {
				for (int xx = 0; xx < skip; xx++) {
					for (int yy = 0; yy < skip; yy++) {
						position = (min((yPositions2[p] + yy), (h / 2) - 1) * (w / 2)
								+ min((xPositions2[p] + xx), (w / 2) - 1));
						positionint = min(max(min(round(position), Integer.MAX_VALUE), 0), w * h / 4 - 1);
						rFRCMAP2[positionint] = values2[p];
					}
				}
			}

			for (int pixel = 0; pixel < w * h / 4; pixel++) {
				if (rFRCMAP[pixel] == 0 || rFRCMAP2[pixel] == 0) {
					rFRCMAP[pixel] = 0;
					rFRCMASK[pixel] = 0;
				} else {
					rFRCMAP[pixel] = (rFRCMAP[pixel] + rFRCMAP2[pixel]) / 2;
				}

			}

			rFRCMAP = AMF(rFRCMAP, w / 2, h / 2);
//			rFRCMAP = AMF(rFRCMAP, w / 2, h / 2);
			counter = 0;
			for (int pixel = 0; pixel < w * h / 4; pixel++) {
				if (rFRCMAP[pixel] == 0)
					;
				else {
					mean += rFRCMAP[pixel];
					counter++;
					max = Math.max(rFRCMAP[pixel], max);
					min = Math.min(rFRCMAP[pixel], min);
				}
			}
			for (int pixel = 0; pixel < w * h / 4; pixel++)
				rFRCMASK[pixel] = max(rFRCMAP[pixel] / (float) min - (float) 1.4, 0);

			FRCData data = new FRCData();
			data.meanFRC = mean /= counter;
			data.maxFRC = max;
			data.minFRC = min;
			data.rFRC = mean / min - 1;
			rt.incrementCounter();
			rt.addValue("Mean (nm)", data.meanFRC);
			rt.addValue("rFRC value", data.rFRC);
			rt.addValue("Min FRC (nm)", data.minFRC);
			rt.addValue("Max FRC (nm)", data.maxFRC);

			rFRCMASK = AMF(rFRCMASK, w / 2, h / 2);
			result.addSlice("", rFRCMAP);
			resultmask.addSlice("", rFRCMASK);

		}
		ImagePlus image = new ImagePlus("rFRC Map (single-frame)", result);
		ImagePlus mask = new ImagePlus("", resultmask);
		ImageConverter ic = new ImageConverter(mask);
		ic.convertToGray8();
		ImageStack msslice = new ImageStack(w / 2, h / 2);
		ImageStack msstack = new ImageStack(w / 2, h / 2);
		ImageProcessor ms;
		for (int slice = 0; slice < mask.getImageStackSize(); slice++) {
			ms = mask.getStack().getProcessor(slice + 1);
			msslice = ostu(ms);
			msstack.addSlice("", msslice.getProcessor(1));
		}

		NJ_LUT.applyLUT_PANEL_rFRC(image);
		image.show();
		String ws = String.valueOf(w);
		String hs = String.valueOf(h);
		String ls = String.valueOf(l);
		String customize = "width=" + ws + " height=" + hs + " depth=" + ls + " constrain average interpolation=None";
		ij.IJ.run("Size...", customize);
		ImagePlus maskshow = new ImagePlus("Simplified PANEL (single-frame rFRC without RSM)", msstack);
		NJ_LUT.applyLUT_PANEL_rFRCmask(maskshow);
//		ij.IJ.run("Calibration Bar...",
//				"location=[Lower Right] fill=Black label=White number=4 decimal=0 font=12 zoom=1 bold");
		maskshow.show();
		ij.IJ.run("Size...", customize);
		rt.show("Single-frame rFRC-Mapping metrics - 3-sigma curve");
	}

	private ArrayList<double[]> rFRC(ImageProcessor ip1s, ImageProcessor ip2s, int blockSize, int backgroundIntensity,
			int skip, float pixelSize) {

		sFRC myFRC = new sFRC();
		int w = ip1s.getWidth();
		int h = ip1s.getHeight();
		int padw = w + blockSize;
		int padh = h + blockSize;

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
				IJ.showStatus("Start single-frame rFRC mapping - 3-sigma curve");
				IJ.showProgress(w * xStart + yStart, w * h);
				FloatProcessor ipROI1 = getROI(ip1, xStart, yStart, blockSize, blockSize);
				FloatProcessor ipROI2 = getROI(ip2, xStart, yStart, blockSize, blockSize);
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

					resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, sFRC.ThresholdMethod.THREE_SIGMA);
					if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
						vectors.add(new double[] { xStart, yStart, resolution * pixelSize });
					} else {
						resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, sFRC.ThresholdMethod.FIVE_SIGMA)
								/ (5 / 3);
						if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
							vectors.add(new double[] { xStart, yStart, resolution * pixelSize });
						} else {
							resolution = myFRC.calculateFireNumber(ipROI1, ipROI2, sFRC.ThresholdMethod.TWO_SIGMA) * 3
									/ 2;
							if (!Double.isNaN(resolution) && !Double.isInfinite(resolution)) {
								vectors.add(new double[] { xStart, yStart, resolution * pixelSize });
							}
						}
					}

				}
			}
		}
		return vectors;
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

		Class<?> clazz = sPANELJ_.class;
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
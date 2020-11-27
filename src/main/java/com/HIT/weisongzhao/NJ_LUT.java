package com.HIT.weisongzhao;

import static com.HIT.weisongzhao.NativeTools.getLocalFileFromResource;

import java.io.File;
import java.io.IOException;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.LUT;

/**
 * Created by paxcalpt on 05/06/2017.
 */
public class NJ_LUT {

	public static void applyLUT(ImagePlus imp, String path) {
		File temp = null;
		try {
			temp = getLocalFileFromResource("/" + path);
		} catch (IOException e) {
			IJ.log("Couldn't find resource: " + path);
		}
		if (temp != null) {
			LUT lut = new LutLoader().openLut(temp.getAbsolutePath());
			imp.setLut(lut);
		}
	}

	public static void applyLUT_PANEL_rFRC(ImagePlus imp) {
		applyLUT(imp, "sJet.lut");
	}

	public static void applyLUT_PANEL_rFRCmask(ImagePlus imp) {
		applyLUT(imp, "PANEL_Green.lut");
	}
}

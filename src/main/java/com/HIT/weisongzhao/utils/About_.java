package com.HIT.weisongzhao.utils;

import javax.swing.JDialog;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class About_ extends JDialog implements PlugIn {
	private String defaultMessage = "(c) 2022 	HIT";
	private WalkBar walk = new WalkBar(this.defaultMessage, true, false, true);

	@Override
	public void run(String arg) {
		this.walk.fillAbout("rFRC mapping and PANEL pinpointing", " 22/09/2022 updated, v0.2.5",
				" rFRC: rolling Fourier Ring Correlation. <br>PANEL: Pixel-level ANalysis of Error Locations.",
				"School of Instrumentation Science and Engineering","Harbin Institute of Technology",
				"Weisong Zhao",
				"<p style=\"text-align:left\">"
						+"<b>It enables SUPER-RESOLUTION scale:</b>"
						+"<br>(i) <b>Data uncertainty mapping </b> of image reconstructions <b>without</b> ground-truth (Reconstruction-1 vs Reconstruction-2);"
						+"<br>(ii) <b>Data uncertainty and leaked model uncertainty mapping</b> of end-to-end deep-learning predictions <b>without</b> ground-truth (Prediction-1 vs Prediction-2);"
						+"<br>(iii) <b>Full error mapping</b> of reconstructions or predictions <b>with</b> ground-truth (Reconstruction or Prediction vs Ground-truth);"
						+"<br>(iv) <b> Resolution mapping</b> of raw images (Image-1 vs Image-2)."					
						+"<br><br><b>It is a part of publication:</b>"
						+ "<br> Weisong Zhao et al. Quantitatively mapping local quality at super-resolution scale by rolling Fourier ring correlation, Nature Methods (2022)."
						+ "<br><br><b>Acknowledgements:</b><br>This plugin is for rFRC mapping and simplified PANEL (w/o RSM) pinpointing. Please cite PANEL in your publications, if it helps your research."
						+ "<br><br><b>Open source:</b><br>https://github.com/WeisongZhao/PANELJ" + "");

		this.walk.showAbout_PANEL();

	}

	public static void main(String[] args) {

		Class<?> clazz = About_.class;

		// start ImageJ
		new ImageJ();

		IJ.runPlugIn(clazz.getName(), "");
	}

}
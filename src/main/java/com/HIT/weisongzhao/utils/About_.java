package com.HIT.weisongzhao.utils;

import javax.swing.JDialog;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class About_ extends JDialog implements PlugIn {
	private String defaultMessage = "(c) 2021 	HIT";
	private WalkBar walk = new WalkBar(this.defaultMessage, true, false, true);

	@Override
	public void run(String arg) {
		this.walk.fillAbout("PANEL error mapping", " 07/19/2021",
				"PANEL: Pixel-level ANalysis of Error Locations <br>v0.2.5",
				"School of Instrumentation Science and Engineering<br/>Harbin Institute of Technology",
				"Weisong Zhao (zhaoweisong950713@163.com)", "2021",
				"<p style=\"text-align:left\">" + "<b>It is a part of publication:</b>"
						+ "<br> Weisong Zhao et al. PANEL: quantitatively mapping reconstruction errors at super-resolution scale by rolling Fourier ring correlation, Nature Methods, X, XXX-XXX (2022)."
						+ "<br><br><b>Acknowledgements:</b><br>This plugin is for Simplified PANEL (w/o RSM). Please cite PANEL in your publications, if it helps your research."
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
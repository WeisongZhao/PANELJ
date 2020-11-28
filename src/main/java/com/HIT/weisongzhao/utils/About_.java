package com.HIT.weisongzhao.utils;

import javax.swing.JDialog
;

import ij.IJ;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class About_ extends JDialog implements PlugIn {
	private String defaultMessage = "(c) 2020 	HIT";
	private WalkBar walk = new WalkBar(this.defaultMessage, true, false, true);

	@Override
	public void run(String arg) {
		this.walk.fillAbout("PANEL error mapping", " 11/26/2020", "PANEL: Pixel-level ANalysis of Error Locations <br>v0.1.5",
				"School of Instrumentation Science and Engineering<br/>Harbin Institute of Technology",
				"Weisong Zhao (zhaoweisong950713@163.com)", "2020",
				"<p style=\"text-align:left\">"
				+ "<b>Publications:</b>"
				+ "<br> Weisong, Zhao, et al. \"[1]. Weisong Zhao, et al. PANEL."
				+ "<br><br><b>References:</b>"
				+ "<br>[1] Nieuwenhuizen, R.P. et al. Measuring image resolution in optical nanoscopy. Nature methods 10, 557 (2013)."
				+ "<br>[2] Culley, S. et al. Quantitative mapping and minimization of super-resolution optical imaging artifacts. Nature methods 15, 263 (2018)."
				+ "<br>[3] https://github.com/aherbert/GDSC-SMLM "
				+ "<br><br><b>Acknowledgements:</b><br>This plugin is for Simplified PANEL (w/o RSM), and the feature single-frame error/resolution mapping is currently not included in PANELJ. "
				+ "<br><br><b>Open source:</b><br>https://github.com/WeisongZhao/PANELJ"
				+ "");
		
		this.walk.showAbout_PANEL();

	}

	public static void main(String[] args) {

		Class<?> clazz = About_.class;

		// start ImageJ
		new ImageJ();

		IJ.runPlugIn(clazz.getName(), "");
	}

}
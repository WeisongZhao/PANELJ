<p>
<h1 align="center">PANEL<font color="#b07219">J</font></h1>
<h5 align="center">Pixel-level ANalysis of Error Locations (or resolution) with FIJI/ImageJ.</h5>
</p>



<p>
<img src='/img/imagej-128.png' align="left" width=100>
</p>

</br>
This repository contains the java source code (Maven) for **PANEL** imagej plugin. This plugin is for the **Simplified PANEL** (w/o RSM), and is also accompanied with resolution mapping (**1/7** golden threshold) feature.
</br>
If you find this useful, please cite the paper.
<b>Weisong Zhao et al. PANEL (2021) .........</b>
</br>
</br>
</br>
>If you want to reproduce the results of PANEL publication, the PANELM is recommended. Due to the distance between the core FRC calculation of PANELJ, and PANELM, and the difference between Fourier transform of Matlab and imagej, there may exist a gap between the results of PANELM and PANELJ. To me, the implementations of core FRC, and Fourier transform in **PANELM**  are more flexible and accurate. This imagej plugin is work in progress, so expect rough edges. 

<p align='center'>
<img src='/img/PANELJ.png' align="center" width=600>
</p>
=
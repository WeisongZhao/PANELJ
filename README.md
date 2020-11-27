<p>
<h1 align="center">PANEL<font color="#b07219">J</font></h1>
<h5 align="center">Pixel-level ANalysis of Error Locations (or resolution) with FIJI/ImageJ.</h5>
</p>
</br>
<p>
<img src='/img/imagej-128.png' align="left" width=110>
</p>

This repository contains the java source code (Maven) for <b>PANEL</b> imagej plugin. This plugin is for the <b>Simplified PANEL</b> (w/o RSM), and is also accompanied with resolution mapping (<b>1/7</b> golden threshold) feature.
</br>
If you find this useful, please cite the paper.
<b>Weisong Zhao et al. PANEL (2021) .........</b>
</br>
</br>
</br>
>If you want to reproduce the results of PANEL publication, the <b>PANELM</b> is recommended. Due to the distance between the core FRC calculation of <b>PANELJ</b>, and <b>PANELM</b>, and the difference between Fourier transform of Matlab and imagej, there may exist a gap between the results of <b>PANELM</b> and <b>PANELJ</b>. To me, the implementations of core FRC, and Fourier transform in <b>PANELM</b>  are more flexible and accurate. This imagej plugin is work in progress, so expect rough edges. 

<p align='center'>
<img src='/img/PANELJ.png' align="center" width=800>
</p>

## PANELM

## Open source [PANELJ](https://github.com/WeisongZhao/PANELJ)
This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
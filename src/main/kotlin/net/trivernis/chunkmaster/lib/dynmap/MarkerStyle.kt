package net.trivernis.chunkmaster.lib.dynmap

import org.dynmap.markers.MarkerIcon

data class MarkerStyle(
    val icon: MarkerIcon?,
    val lineStyle: LineStyle?,
    val fillStyle: FillStyle?,
    val boostFlag: Boolean = false
)
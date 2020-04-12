package net.trivernis.chunkmaster.lib.dynmap

import org.bukkit.Location
import org.dynmap.markers.AreaMarker
import org.dynmap.markers.Marker
import org.dynmap.markers.MarkerSet

class ExtendedMarkerSet(private val markerSet: MarkerSet) {
    /**
     * Creates or updates an area marker depending on if it exists
     * @param id - the unique id of the area marker
     * @param label - the label that is displayed when clicking on the marker
     * @param l1 - the top left corner
     * @param l2 - the bottom right corner
     */
    fun creUpdateAreMarker(id: String, label: String, l1: Location, l2: Location, style: MarkerStyle?): AreaMarker {
        var marker = markerSet.findAreaMarker(id)

        if (marker != null) {
            marker.setCornerLocations(
                doubleArrayOf(l1.x, l2.x),
                doubleArrayOf(l1.z, l2.z)
            )
        } else {
            marker = markerSet.createAreaMarker(
                id,
                label,
                false,
                l1.world.name,
                doubleArrayOf(l1.x, l2.x),
                doubleArrayOf(l1.z, l2.z),
                true
            )
        }
        if (style != null) {
            marker.boostFlag = style.boostFlag
            if (style.lineStyle != null) {
                marker.setLineStyle(style.lineStyle.weight, style.lineStyle.opacity, style.lineStyle.color)
            }
            if (style.fillStyle != null) {
                marker.setFillStyle(style.fillStyle.opacity, style.fillStyle.color)
            }
        }
        return marker
    }

    /**
     * Returns the area marker for an id
     * @param id - the id of the marker
     */
    fun findAreaMarker(id: String): AreaMarker? {
        return markerSet.findAreaMarker(id)
    }

    /**
     * Deletes an area marker
     * @param id - the id of the marker
     */
    fun deleteAreaMarker(id: String) {
        val marker = this.findAreaMarker(id)
        marker?.deleteMarker()
    }
}
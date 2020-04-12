package net.trivernis.chunkmaster.lib.dynmap

import org.dynmap.DynmapAPI

class DynmapApiWrapper(private val dynmapAPI: DynmapAPI) {

    /**
     * Returns a marker set by name
     */
    fun getMarkerSet(name: String): ExtendedMarkerSet? {
        val set = dynmapAPI.markerAPI?.getMarkerSet(name)
        return if (set != null) {
            ExtendedMarkerSet(set)
        } else {
            null
        }
    }

    fun getCreateMarkerSet(id: String, name: String): ExtendedMarkerSet? {
        var set = dynmapAPI.markerAPI?.getMarkerSet(id)
        if (set == null) {
            set = dynmapAPI.markerAPI?.createMarkerSet(id, name, null, true)
        }
        return if (set != null) {
            ExtendedMarkerSet(set)
        } else {
            null
        }
    }
}
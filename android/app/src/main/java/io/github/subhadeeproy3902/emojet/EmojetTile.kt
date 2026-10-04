package io.github.subhadeeproy3902.emojet

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Quick-settings tile: one tap turns EmoJet suggestions on or off (also ends a pause). */
class EmojetTile : TileService() {

    override fun onStartListening() = update()

    override fun onClick() {
        val on = !Prefs.activeFor(this, null)
        Prefs.setEnabled(this, on)
        if (on) Prefs.resume(this)
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val active = Prefs.activeFor(this, null)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "EmoJet"
        if (Build.VERSION.SDK_INT >= 29) tile.subtitle = if (active) "Suggestions on" else "Off"
        tile.updateTile()
    }
}

package cz.cuni.gamedev.nail123.roguelike.events

class LoggedEvent(override val emitter: Any, val logMessage: String): GameEvent()
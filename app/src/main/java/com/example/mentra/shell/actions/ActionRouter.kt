package com.example.mentra.shell.actions

import com.example.mentra.shell.models.ShellAction
import com.example.mentra.shell.models.ShellResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Routes actions to appropriate handlers
 */
@Singleton
class ActionRouter @Inject constructor(
    private val systemActionHandler: SystemActionHandler,
    private val advancedSystemActionHandler: AdvancedSystemActionHandler,
    private val queryActionHandler: QueryActionHandler,
    private val callActionHandler: CallActionHandler,
    private val messageActionHandler: MessageActionHandler,
    private val mediaActionHandler: MediaActionHandler,
    private val fileActionHandler: FileActionHandler
) {

    /**
     * Route action to appropriate handler
     */
    suspend fun route(action: ShellAction): ShellResult {
        return when (action.type) {
            // System-wide commands (highest priority)
            com.example.mentra.shell.models.ActionType.SYSTEM_COMMAND ->
                advancedSystemActionHandler.handle(action)

            // System actions
            com.example.mentra.shell.models.ActionType.OPEN_APP,
            com.example.mentra.shell.models.ActionType.LAUNCH_ACTIVITY,
            com.example.mentra.shell.models.ActionType.OPEN_SETTINGS ->
                systemActionHandler.handle(action)

            // Communication actions
            com.example.mentra.shell.models.ActionType.MAKE_CALL ->
                callActionHandler.handle(action)

            com.example.mentra.shell.models.ActionType.SEND_SMS,
            com.example.mentra.shell.models.ActionType.SEND_MMS ->
                messageActionHandler.handle(action)

            // Media actions
            com.example.mentra.shell.models.ActionType.PLAY_MUSIC,
            com.example.mentra.shell.models.ActionType.PLAY_VIDEO,
            com.example.mentra.shell.models.ActionType.PAUSE_MEDIA,
            com.example.mentra.shell.models.ActionType.STOP_MEDIA,
            com.example.mentra.shell.models.ActionType.NEXT_TRACK,
            com.example.mentra.shell.models.ActionType.PREVIOUS_TRACK ->
                mediaActionHandler.handle(action)

            // Query actions
            com.example.mentra.shell.models.ActionType.SHOW_BATTERY,
            com.example.mentra.shell.models.ActionType.SHOW_STORAGE,
            com.example.mentra.shell.models.ActionType.SHOW_NETWORK,
            com.example.mentra.shell.models.ActionType.SHOW_TIME,
            com.example.mentra.shell.models.ActionType.SHOW_DATE,
            com.example.mentra.shell.models.ActionType.SHOW_STEPS,
            com.example.mentra.shell.models.ActionType.SHOW_DEVICE ->
                queryActionHandler.handle(action)

            // File actions
            com.example.mentra.shell.models.ActionType.LIST_FILES,
            com.example.mentra.shell.models.ActionType.READ_FILE,
            com.example.mentra.shell.models.ActionType.WRITE_FILE,
            com.example.mentra.shell.models.ActionType.DELETE_FILE ->
                fileActionHandler.handle(action)

            else -> ShellResult(
                status = com.example.mentra.shell.models.ResultStatus.INVALID_COMMAND,
                message = "Unknown action type: ${action.type}"
            )
        }
    }
}

/**
 * Base interface for action handlers
 */
interface ActionHandler {
    suspend fun handle(action: ShellAction): ShellResult
}


package com.funkforge.backend

import android.content.Context
import androidx.compose.runtime.*
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String
) {
    ENGLISH("en-US", "English"),
    PORTUGUESE_BRAZIL("pt-BR", "Português (Brasil)");

    companion object {
        fun fromCode(code: String?): AppLanguage {
            return entries.firstOrNull {
                it.code.equals(code, ignoreCase = true)
            } ?: ENGLISH
        }

        fun systemDefault(): AppLanguage {
            val language = Locale.getDefault().language
            val country = Locale.getDefault().country

            return if (
                language.equals("pt", ignoreCase = true) &&
                country.equals("BR", ignoreCase = true)
            ) {
                PORTUGUESE_BRAZIL
            } else {
                ENGLISH
            }
        }
    }
}

enum class TextKey {
    APP_NAME,
    APP_SUBTITLE,
    APP_SLOGAN,
    HOME,
    WORKSPACE,
    TOOLS,
    MORE,
    CHART_EDITOR,
    CHART_EDITOR_DESCRIPTION,
    CONTINUE_PROJECT,
    CONTINUE_PROJECT_DESCRIPTION,
    NEW_PROJECT,
    NEW_PROJECT_DESCRIPTION,
    IMPORT_CHART,
    IMPORT_CHART_DESCRIPTION,
    MOD_PLAYTEST,
    MOD_PLAYTEST_DESCRIPTION,
    FUNKY_MAKER_CONVERTER,
    COMING_SOON,
    SECRET_ZONE,
    SECRET_ZONE_DESCRIPTION,
    OPTIONS,
    SETTINGS,
    DISPLAY,
    AUDIO,
    GAMEPLAY,
    LANGUAGE,
    LANGUAGE_DESCRIPTION,
    ENGLISH,
    PORTUGUESE_BRAZIL,
    SHOW_FPS,
    SHOW_FPS_DESCRIPTION,
    REDUCE_MOTION,
    REDUCE_MOTION_DESCRIPTION,
    ABOUT,
    VERSION,
    BACK,
    CANCEL,
    SAVE,
    DELETE,
    CONFIRM,
    CLOSE,
    APPLY,
    RESET,
    ENABLED,
    DISABLED,
    YES,
    NO,
    PLAY,
    PAUSE,
    RESUME,
    STOP,
    RESTART,
    EXIT,
    RETRY,
    IMPORT,
    EXPORT,
    EXPORT_JSON,
    EXPORT_MOD,
    IMPORT_INST,
    IMPORT_VOICES,
    INSTRUMENTAL,
    VOCALS,
    SONG,
    PROJECT,
    NOTES,
    NOTE,
    BEAT,
    STEP,
    BPM,
    SCROLL_SPEED,
    SNAP,
    ZOOM_IN,
    ZOOM_OUT,
    UNDO,
    REDO,
    DRAW,
    SELECT,
    ERASE,
    SELECTED_NOTE,
    SUSTAIN,
    TIME,
    LANE,
    MOVE_EARLIER,
    MOVE_LATER,
    SHORTEN_SUSTAIN,
    EXTEND_SUSTAIN,
    DELETE_NOTE,
    DESELECT,
    CHART_PROPERTIES,
    PROJECT_SETTINGS,
    PREVIOUS_BEAT,
    NEXT_BEAT,
    PLAYTEST,
    SCORE,
    COMBO,
    MAX_COMBO,
    ACCURACY,
    MISSES,
    RANK,
    HEALTH,
    SICK,
    GOOD,
    BAD,
    SHIT,
    MISS,
    GAME_OVER,
    SONG_COMPLETE,
    PAUSED,
    COUNTDOWN,
    PLAYER,
    OPPONENT,
    STAGE,
    DIFFICULTY,
    EVENTS,
    EVENT_EDITOR,
    ADD_EVENT,
    REMOVE_EVENT,
    NO_PROJECT,
    NO_AUDIO,
    INVALID_BPM,
    INVALID_SPEED,
    INVALID_FILE,
    FILE_NOT_FOUND,
    IMPORT_SUCCESS,
    IMPORT_FAILED,
    EXPORT_SUCCESS,
    EXPORT_FAILED,
    PROJECT_SAVED,
    SAVE_FAILED,
    INST_LOADED,
    VOICES_LOADED,
    AUDIO_IMPORT_FAILED,
    MOD_EXPORTED,
    MOD_EXPORT_FAILED,
    CONVERT_AUDIO_WARNING,
    UNSUPPORTED_AUDIO,
    PERMISSION_DENIED,
    UNKNOWN_ERROR,
    LOADING,
    SAVING,
    EXPORTING,
    IMPORTING,
    READY,
    MADE_FOR_MOBILE
}

object Language {

    private const val PREFS_NAME = "funkforge_options"
    private const val LANGUAGE_KEY = "language"

    private val english = mapOf(
        TextKey.APP_NAME to "FunkForge",
        TextKey.APP_SUBTITLE to "Your Friday Night Funkin' creation studio.",
        TextKey.APP_SLOGAN to "CREATE • EDIT • REMIX",
        TextKey.HOME to "Home",
        TextKey.WORKSPACE to "Workspace",
        TextKey.TOOLS to "Tools",
        TextKey.MORE to "More",
        TextKey.CHART_EDITOR to "Chart Editor",
        TextKey.CHART_EDITOR_DESCRIPTION to "Create and edit your charts",
        TextKey.CONTINUE_PROJECT to "Continue Project",
        TextKey.CONTINUE_PROJECT_DESCRIPTION to "Resume your saved project",
        TextKey.NEW_PROJECT to "New Project",
        TextKey.NEW_PROJECT_DESCRIPTION to "Create your next mod",
        TextKey.IMPORT_CHART to "Import Chart",
        TextKey.IMPORT_CHART_DESCRIPTION to "Import a Psych Engine JSON chart",
        TextKey.MOD_PLAYTEST to "Mod Playtest",
        TextKey.MOD_PLAYTEST_DESCRIPTION to "Test songs from a mod folder",
        TextKey.FUNKY_MAKER_CONVERTER to "Funky Maker Converter",
        TextKey.COMING_SOON to "Coming Soon",
        TextKey.SECRET_ZONE to "Secret Zone",
        TextKey.SECRET_ZONE_DESCRIPTION to "Discover hidden FunkForge content",
        TextKey.OPTIONS to "Options",
        TextKey.SETTINGS to "Settings",
        TextKey.DISPLAY to "Display",
        TextKey.AUDIO to "Audio",
        TextKey.GAMEPLAY to "Gameplay",
        TextKey.LANGUAGE to "Language",
        TextKey.LANGUAGE_DESCRIPTION to "Choose your preferred language",
        TextKey.ENGLISH to "English",
        TextKey.PORTUGUESE_BRAZIL to "Portuguese (Brazil)",
        TextKey.SHOW_FPS to "Show FPS",
        TextKey.SHOW_FPS_DESCRIPTION to "Display the FPS counter",
        TextKey.REDUCE_MOTION to "Reduce Motion",
        TextKey.REDUCE_MOTION_DESCRIPTION to "Reduce animated effects",
        TextKey.ABOUT to "About",
        TextKey.VERSION to "Version",
        TextKey.BACK to "Back",
        TextKey.CANCEL to "Cancel",
        TextKey.SAVE to "Save",
        TextKey.DELETE to "Delete",
        TextKey.CONFIRM to "Confirm",
        TextKey.CLOSE to "Close",
        TextKey.APPLY to "Apply",
        TextKey.RESET to "Reset",
        TextKey.ENABLED to "Enabled",
        TextKey.DISABLED to "Disabled",
        TextKey.YES to "Yes",
        TextKey.NO to "No",
        TextKey.PLAY to "Play",
        TextKey.PAUSE to "Pause",
        TextKey.RESUME to "Resume",
        TextKey.STOP to "Stop",
        TextKey.RESTART to "Restart",
        TextKey.EXIT to "Exit",
        TextKey.RETRY to "Retry",
        TextKey.IMPORT to "Import",
        TextKey.EXPORT to "Export",
        TextKey.EXPORT_JSON to "Export JSON",
        TextKey.EXPORT_MOD to "Export Mod",
        TextKey.IMPORT_INST to "Import Inst",
        TextKey.IMPORT_VOICES to "Import Voices",
        TextKey.INSTRUMENTAL to "Instrumental",
        TextKey.VOCALS to "Vocals",
        TextKey.SONG to "Song",
        TextKey.PROJECT to "Project",
        TextKey.NOTES to "Notes",
        TextKey.NOTE to "Note",
        TextKey.BEAT to "Beat",
        TextKey.STEP to "Step",
        TextKey.BPM to "BPM",
        TextKey.SCROLL_SPEED to "Scroll Speed",
        TextKey.SNAP to "Snap",
        TextKey.ZOOM_IN to "Zoom In",
        TextKey.ZOOM_OUT to "Zoom Out",
        TextKey.UNDO to "Undo",
        TextKey.REDO to "Redo",
        TextKey.DRAW to "Draw",
        TextKey.SELECT to "Select",
        TextKey.ERASE to "Erase",
        TextKey.SELECTED_NOTE to "Selected Note",
        TextKey.SUSTAIN to "Sustain",
        TextKey.TIME to "Time",
        TextKey.LANE to "Lane",
        TextKey.MOVE_EARLIER to "Move Earlier",
        TextKey.MOVE_LATER to "Move Later",
        TextKey.SHORTEN_SUSTAIN to "Shorten Sustain",
        TextKey.EXTEND_SUSTAIN to "Extend Sustain",
        TextKey.DELETE_NOTE to "Delete Note",
        TextKey.DESELECT to "Deselect",
        TextKey.CHART_PROPERTIES to "Chart Properties",
        TextKey.PROJECT_SETTINGS to "Project Settings",
        TextKey.PREVIOUS_BEAT to "Previous Beat",
        TextKey.NEXT_BEAT to "Next Beat",
        TextKey.PLAYTEST to "Playtest",
        TextKey.SCORE to "Score",
        TextKey.COMBO to "Combo",
        TextKey.MAX_COMBO to "Max Combo",
        TextKey.ACCURACY to "Accuracy",
        TextKey.MISSES to "Misses",
        TextKey.RANK to "Rank",
        TextKey.HEALTH to "Health",
        TextKey.SICK to "Sick",
        TextKey.GOOD to "Good",
        TextKey.BAD to "Bad",
        TextKey.SHIT to "Shit",
        TextKey.MISS to "Miss",
        TextKey.GAME_OVER to "Game Over",
        TextKey.SONG_COMPLETE to "Song Complete",
        TextKey.PAUSED to "Paused",
        TextKey.COUNTDOWN to "Countdown",
        TextKey.PLAYER to "Player",
        TextKey.OPPONENT to "Opponent",
        TextKey.STAGE to "Stage",
        TextKey.DIFFICULTY to "Difficulty",
        TextKey.EVENTS to "Events",
        TextKey.EVENT_EDITOR to "Event Editor",
        TextKey.ADD_EVENT to "Add Event",
        TextKey.REMOVE_EVENT to "Remove Event",
        TextKey.NO_PROJECT to "No project found",
        TextKey.NO_AUDIO to "Import an audio file first",
        TextKey.INVALID_BPM to "Invalid BPM",
        TextKey.INVALID_SPEED to "Invalid speed",
        TextKey.INVALID_FILE to "Invalid file",
        TextKey.FILE_NOT_FOUND to "File not found",
        TextKey.IMPORT_SUCCESS to "Imported successfully",
        TextKey.IMPORT_FAILED to "Import failed",
        TextKey.EXPORT_SUCCESS to "Exported successfully",
        TextKey.EXPORT_FAILED to "Export failed",
        TextKey.PROJECT_SAVED to "Project saved",
        TextKey.SAVE_FAILED to "Failed to save project",
        TextKey.INST_LOADED to "Instrumental loaded",
        TextKey.VOICES_LOADED to "Vocals loaded",
        TextKey.AUDIO_IMPORT_FAILED to "Audio import failed",
        TextKey.MOD_EXPORTED to "Mod exported successfully",
        TextKey.MOD_EXPORT_FAILED to "Mod export failed",
        TextKey.CONVERT_AUDIO_WARNING to "Convert audio to OGG for Psych Engine",
        TextKey.UNSUPPORTED_AUDIO to "Unsupported audio format",
        TextKey.PERMISSION_DENIED to "Permission denied",
        TextKey.UNKNOWN_ERROR to "Unknown error",
        TextKey.LOADING to "Loading...",
        TextKey.SAVING to "Saving...",
        TextKey.EXPORTING to "Exporting...",
        TextKey.IMPORTING to "Importing...",
        TextKey.READY to "Ready",
        TextKey.MADE_FOR_MOBILE to "MADE FOR MOBILE • KEEP FUNKIN'"
    )

    private val portugueseBrazil = mapOf(
        TextKey.APP_NAME to "FunkForge",
        TextKey.APP_SUBTITLE to "Seu estúdio de criação de Friday Night Funkin'.",
        TextKey.APP_SLOGAN to "CRIE • EDITE • REMIXE",
        TextKey.HOME to "Início",
        TextKey.WORKSPACE to "Área de trabalho",
        TextKey.TOOLS to "Ferramentas",
        TextKey.MORE to "Mais",
        TextKey.CHART_EDITOR to "Editor de Charts",
        TextKey.CHART_EDITOR_DESCRIPTION to "Crie e edite seus charts",
        TextKey.CONTINUE_PROJECT to "Continuar Projeto",
        TextKey.CONTINUE_PROJECT_DESCRIPTION to "Continue seu projeto salvo",
        TextKey.NEW_PROJECT to "Novo Projeto",
        TextKey.NEW_PROJECT_DESCRIPTION to "Crie seu próximo mod",
        TextKey.IMPORT_CHART to "Importar Chart",
        TextKey.IMPORT_CHART_DESCRIPTION to "Importe um chart JSON do Psych Engine",
        TextKey.MOD_PLAYTEST to "Testar Mod",
        TextKey.MOD_PLAYTEST_DESCRIPTION to "Teste músicas de uma pasta de mod",
        TextKey.FUNKY_MAKER_CONVERTER to "Conversor Funky Maker",
        TextKey.COMING_SOON to "Em breve",
        TextKey.SECRET_ZONE to "Zona Secreta",
        TextKey.SECRET_ZONE_DESCRIPTION to "Descubra conteúdos secretos da FunkForge",
        TextKey.OPTIONS to "Opções",
        TextKey.SETTINGS to "Configurações",
        TextKey.DISPLAY to "Exibição",
        TextKey.AUDIO to "Áudio",
        TextKey.GAMEPLAY to "Jogabilidade",
        TextKey.LANGUAGE to "Idioma",
        TextKey.LANGUAGE_DESCRIPTION to "Escolha seu idioma preferido",
        TextKey.ENGLISH to "Inglês",
        TextKey.PORTUGUESE_BRAZIL to "Português (Brasil)",
        TextKey.SHOW_FPS to "Mostrar FPS",
        TextKey.SHOW_FPS_DESCRIPTION to "Exibir o contador de FPS",
        TextKey.REDUCE_MOTION to "Reduzir Animações",
        TextKey.REDUCE_MOTION_DESCRIPTION to "Reduzir os efeitos animados",
        TextKey.ABOUT to "Sobre",
        TextKey.VERSION to "Versão",
        TextKey.BACK to "Voltar",
        TextKey.CANCEL to "Cancelar",
        TextKey.SAVE to "Salvar",
        TextKey.DELETE to "Excluir",
        TextKey.CONFIRM to "Confirmar",
        TextKey.CLOSE to "Fechar",
        TextKey.APPLY to "Aplicar",
        TextKey.RESET to "Redefinir",
        TextKey.ENABLED to "Ativado",
        TextKey.DISABLED to "Desativado",
        TextKey.YES to "Sim",
        TextKey.NO to "Não",
        TextKey.PLAY to "Reproduzir",
        TextKey.PAUSE to "Pausar",
        TextKey.RESUME to "Continuar",
        TextKey.STOP to "Parar",
        TextKey.RESTART to "Reiniciar",
        TextKey.EXIT to "Sair",
        TextKey.RETRY to "Tentar novamente",
        TextKey.IMPORT to "Importar",
        TextKey.EXPORT to "Exportar",
        TextKey.EXPORT_JSON to "Exportar JSON",
        TextKey.EXPORT_MOD to "Exportar Mod",
        TextKey.IMPORT_INST to "Importar Instrumental",
        TextKey.IMPORT_VOICES to "Importar Vocais",
        TextKey.INSTRUMENTAL to "Instrumental",
        TextKey.VOCALS to "Vocais",
        TextKey.SONG to "Música",
        TextKey.PROJECT to "Projeto",
        TextKey.NOTES to "Notas",
        TextKey.NOTE to "Nota",
        TextKey.BEAT to "Batida",
        TextKey.STEP to "Passo",
        TextKey.BPM to "BPM",
        TextKey.SCROLL_SPEED to "Velocidade das Notas",
        TextKey.SNAP to "Divisão",
        TextKey.ZOOM_IN to "Aumentar Zoom",
        TextKey.ZOOM_OUT to "Diminuir Zoom",
        TextKey.UNDO to "Desfazer",
        TextKey.REDO to "Refazer",
        TextKey.DRAW to "Desenhar",
        TextKey.SELECT to "Selecionar",
        TextKey.ERASE to "Apagar",
        TextKey.SELECTED_NOTE to "Nota Selecionada",
        TextKey.SUSTAIN to "Nota Longa",
        TextKey.TIME to "Tempo",
        TextKey.LANE to "Coluna",
        TextKey.MOVE_EARLIER to "Mover para Trás",
        TextKey.MOVE_LATER to "Mover para Frente",
        TextKey.SHORTEN_SUSTAIN to "Encurtar Nota Longa",
        TextKey.EXTEND_SUSTAIN to "Aumentar Nota Longa",
        TextKey.DELETE_NOTE to "Excluir Nota",
        TextKey.DESELECT to "Desmarcar",
        TextKey.CHART_PROPERTIES to "Propriedades do Chart",
        TextKey.PROJECT_SETTINGS to "Configurações do Projeto",
        TextKey.PREVIOUS_BEAT to "Batida Anterior",
        TextKey.NEXT_BEAT to "Próxima Batida",
        TextKey.PLAYTEST to "Testar",
        TextKey.SCORE to "Pontuação",
        TextKey.COMBO to "Combo",
        TextKey.MAX_COMBO to "Combo Máximo",
        TextKey.ACCURACY to "Precisão",
        TextKey.MISSES to "Erros",
        TextKey.RANK to "Classificação",
        TextKey.HEALTH to "Vida",
        TextKey.SICK to "Perfeito",
        TextKey.GOOD to "Bom",
        TextKey.BAD to "Ruim",
        TextKey.SHIT to "Péssimo",
        TextKey.MISS to "Erro",
        TextKey.GAME_OVER to "Fim de Jogo",
        TextKey.SONG_COMPLETE to "Música Concluída",
        TextKey.PAUSED to "Pausado",
        TextKey.COUNTDOWN to "Contagem Regressiva",
        TextKey.PLAYER to "Jogador",
        TextKey.OPPONENT to "Oponente",
        TextKey.STAGE to "Cenário",
        TextKey.DIFFICULTY to "Dificuldade",
        TextKey.EVENTS to "Eventos",
        TextKey.EVENT_EDITOR to "Editor de Eventos",
        TextKey.ADD_EVENT to "Adicionar Evento",
        TextKey.REMOVE_EVENT to "Remover Evento",
        TextKey.NO_PROJECT to "Nenhum projeto encontrado",
        TextKey.NO_AUDIO to "Importe um arquivo de áudio primeiro",
        TextKey.INVALID_BPM to "BPM inválido",
        TextKey.INVALID_SPEED to "Velocidade inválida",
        TextKey.INVALID_FILE to "Arquivo inválido",
        TextKey.FILE_NOT_FOUND to "Arquivo não encontrado",
        TextKey.IMPORT_SUCCESS to "Importado com sucesso",
        TextKey.IMPORT_FAILED to "Falha ao importar",
        TextKey.EXPORT_SUCCESS to "Exportado com sucesso",
        TextKey.EXPORT_FAILED to "Falha ao exportar",
        TextKey.PROJECT_SAVED to "Projeto salvo",
        TextKey.SAVE_FAILED to "Falha ao salvar o projeto",
        TextKey.INST_LOADED to "Instrumental carregado",
        TextKey.VOICES_LOADED to "Vocais carregados",
        TextKey.AUDIO_IMPORT_FAILED to "Falha ao importar áudio",
        TextKey.MOD_EXPORTED to "Mod exportado com sucesso",
        TextKey.MOD_EXPORT_FAILED to "Falha ao exportar o mod",
        TextKey.CONVERT_AUDIO_WARNING to "Converta o áudio para OGG no Psych Engine",
        TextKey.UNSUPPORTED_AUDIO to "Formato de áudio não suportado",
        TextKey.PERMISSION_DENIED to "Permissão negada",
        TextKey.UNKNOWN_ERROR to "Erro desconhecido",
        TextKey.LOADING to "Carregando...",
        TextKey.SAVING to "Salvando...",
        TextKey.EXPORTING to "Exportando...",
        TextKey.IMPORTING to "Importando...",
        TextKey.READY to "Pronto",
        TextKey.MADE_FOR_MOBILE to "FEITO PARA MOBILE • KEEP FUNKIN'"
    )

    private var currentLanguage by mutableStateOf(
        AppLanguage.ENGLISH
    )

    val current: AppLanguage
        get() = currentLanguage

    val availableLanguages: List<AppLanguage>
        get() = AppLanguage.entries

    fun initialize(context: Context) {
        val preferences = context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )

        val saved = preferences.getString(
            LANGUAGE_KEY,
            null
        )

        currentLanguage = if (saved != null) {
            AppLanguage.fromCode(saved)
        } else {
            AppLanguage.systemDefault()
        }
    }

    fun setLanguage(
        context: Context,
        language: AppLanguage
    ) {
        currentLanguage = language

        context.applicationContext
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(LANGUAGE_KEY, language.code)
            .apply()
    }

    fun get(key: TextKey): String {
        val translations = when (currentLanguage) {
            AppLanguage.ENGLISH -> english
            AppLanguage.PORTUGUESE_BRAZIL -> portugueseBrazil
        }

        return translations[key]
            ?: english[key]
            ?: key.name
                .replace("_", " ")
                .lowercase()
                .replaceFirstChar {
                    it.titlecase()
                }
    }

    fun get(
        key: TextKey,
        vararg arguments: Any
    ): String {
        val value = get(key)

        return runCatching {
            String.format(
                Locale.forLanguageTag(currentLanguage.code),
                value,
                *arguments
            )
        }.getOrDefault(value)
    }

    fun isPortuguese(): Boolean {
        return currentLanguage == AppLanguage.PORTUGUESE_BRAZIL
    }

    fun isEnglish(): Boolean {
        return currentLanguage == AppLanguage.ENGLISH
    }
}

@Composable
fun translated(key: TextKey): String {
    return Language.get(key)
}

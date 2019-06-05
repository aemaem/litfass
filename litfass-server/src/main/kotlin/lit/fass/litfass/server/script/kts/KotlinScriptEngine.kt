package lit.fass.litfass.server.script.kts

import lit.fass.litfass.server.script.ScriptEngine
import lit.fass.litfass.server.script.ScriptLanguage
import lit.fass.litfass.server.script.ScriptLanguage.KOTLIN
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

/**
 * WARNING: This implementation causes a memory leak. Use the Groovy script engine instead. todo: fix memory leak
 *
 * @author Michael Mair
 */
class KotlinScriptEngine : ScriptEngine {
    companion object {
        private val lang = KOTLIN
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val scriptLog = LoggerFactory.getLogger("$lang.Script")
        private val scriptTimestampFormatter = ISO_DATE_TIME.withZone(UTC)
    }

    private val classPath: List<String> = System.getProperty("java.class.path")
        .splitToSequence(":")
        .filter {
            !it.contains("*") && (
                    it.contains("kotlin-script-util") ||
                            it.contains("kotlin-script-runtime") ||
                            it.contains("kotlin-stdlib") ||
                            it.contains("slf4j-api") ||
                            it.contains("ext/")
                    )
        }
        .toList()

    init {
        log.debug("Adding classpath $classPath")
        System.setProperty("kotlin.script.classpath", classPath.joinToString(":"))
    }

    override fun isApplicable(language: ScriptLanguage): Boolean {
        return lang == language
    }

    override fun invoke(script: String, data: Map<String, Any?>): Map<String, Any?> {
        log.trace("Invoking script:\n$script\nwith data \n$data")
        var result: Map<String, Any?> = emptyMap()
        val elapsedTime = measureTimeMillis {
            URLClassLoader(classPath.map { URL("file:$it") }.toTypedArray()).use { classLoader ->
                result = with(ScriptEngineManager(classLoader).getEngineByName(lang.name.toLowerCase())) {
                    @Suppress("UNCHECKED_CAST")
                    eval(script, createBindings().apply {
                        put("log", scriptLog)
                        put("timestampFormatter", scriptTimestampFormatter)
                        put("data", data)
                    }) as Map<String, Any?>
                }
            }
        }
        log.debug("Script executed in ${formatDurationHMS(elapsedTime)}")
        return result
    }
}
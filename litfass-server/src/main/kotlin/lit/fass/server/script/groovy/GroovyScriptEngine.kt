package lit.fass.server.script.groovy

import  groovy.lang.GroovyClassLoader
import lit.fass.server.script.ScriptEngine
import lit.fass.server.script.ScriptLanguage
import lit.fass.server.script.ScriptLanguage.GROOVY
import org.apache.commons.lang3.time.DurationFormatUtils.formatDurationHMS
import org.slf4j.LoggerFactory
import java.net.URL
import java.net.URLClassLoader
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter.ISO_DATE_TIME
import javax.script.ScriptEngineManager
import kotlin.system.measureTimeMillis

/**
 * @author Michael Mair
 */
class GroovyScriptEngine : ScriptEngine {
    companion object {
        private val lang = GROOVY
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private val scriptLog = LoggerFactory.getLogger("$lang.Script")
        private val scriptTimestampFormatter = ISO_DATE_TIME.withZone(UTC)
    }

    private val classPath: List<String> = System.getProperty("java.class.path")
        .splitToSequence(":")
        .filter {
            !it.contains("*") && (
                    it.contains("groovy") ||
                            it.contains("slf4j-api") ||
                            it.contains("ext/")
                    )
        }
        .toList()

    init {
        log.debug("Adding classpath $classPath")
    }

    override fun isApplicable(language: ScriptLanguage): Boolean {
        return lang == language
    }

    override fun invoke(script: String, data: Collection<Map<String, Any?>>): Collection<Map<String, Any?>> {
        log.trace("Invoking script:\n$script\nwith data \n$data")
        var result: Collection<Map<String, Any?>> = listOf(emptyMap())
        val elapsedTime = measureTimeMillis {
            GroovyClassLoader(URLClassLoader(classPath.map { URL("file:$it") }.toTypedArray())).use { classLoader ->
                result = with(ScriptEngineManager(classLoader).getEngineByName(lang.name.toLowerCase())) {
                    val evalResult = eval(script, createBindings().apply {
                        put("log", scriptLog)
                        put("timestampFormatter", scriptTimestampFormatter)
                        if (data.size == 1) {
                            put("data", data.first())
                        } else {
                            put("data", data)
                        }
                    })

                    when (evalResult) {
                        is Collection<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            evalResult as Collection<Map<String, Any?>>
                        }
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            listOf(evalResult as Map<String, Any?>)
                        }
                        else -> emptyList()
                    }
                }
            }
        }
        log.debug("Script executed in ${formatDurationHMS(elapsedTime)}")
        return result
    }
}
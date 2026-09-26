package ro.buroot.laqta

import java.io.OutputStreamWriter
import java.util.concurrent.TimeUnit

object RootShell {

    data class Result(val code: Int, val out: String)

    @Volatile
    var cachedRoot: Boolean? = null
        private set

    private var process: Process? = null
    private var writer: OutputStreamWriter? = null

    fun exec(cmd: String, timeoutMs: Long = 8000): Result {
        return try {
            val p = ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start()
            val out = StringBuilder()
            val reader = Thread {
                try {
                    p.inputStream.bufferedReader().forEachLine { out.appendLine(it) }
                } catch (_: Exception) {
                }
            }
            reader.start()
            val finished = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                p.destroy()
                return Result(-1, "timeout")
            }
            reader.join(500)
            Result(p.exitValue(), out.toString().trim())
        } catch (e: Exception) {
            Result(-1, e.message ?: "error")
        }
    }

    fun hasRoot(): Boolean {
        val r = exec("id")
        val ok = r.code == 0 && r.out.contains("uid=0")
        cachedRoot = ok
        return ok
    }

    @Synchronized
    fun fire(cmd: String): Boolean {
        if (cachedRoot == false) return false
        repeat(2) {
            try {
                val w = ensureShell()
                w.write("( $cmd ) >/dev/null 2>&1\n")
                w.flush()
                return true
            } catch (_: Exception) {
                closeShell()
            }
        }
        return false
    }

    private fun ensureShell(): OutputStreamWriter {
        val p = process
        val w = writer
        if (p != null && w != null && p.isAlive) return w
        closeShell()
        val np = ProcessBuilder("su").redirectErrorStream(true).start()
        Thread {
            try {
                val buf = ByteArray(1024)
                val ins = np.inputStream
                while (ins.read(buf) != -1) {
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true }.start()
        val nw = OutputStreamWriter(np.outputStream)
        process = np
        writer = nw
        return nw
    }

    private fun closeShell() {
        try {
            writer?.close()
        } catch (_: Exception) {
        }
        try {
            process?.destroy()
        } catch (_: Exception) {
        }
        writer = null
        process = null
    }
}

import javafx.application.Application
import java.io.File

class LaunchUI {
    companion object {
        lateinit var params: Array<String>
        @JvmStatic
        fun main(args: Array<String>) {
            params = args
            Application.launch(ImageCalcStart::class.java, *args)
        }
    }
}
package ljs.xposed.noreboot

import com.google.gson.annotations.Expose
import org.junit.Test

open class User {
    @Expose(serialize = false, deserialize = false)
    var name = ""
    var sex = ""
}

open class Test {
    @Test
    fun test() {
        val user = User()
        user.name = "ljs"
        user.sex = "man"
        val json = user.toJson()
        println(json)
    }

    @Test
    fun test1() {
        val jsonStr = "[]"
        val modules = jsonStr.toModuleList()
        print(modules)
    }

    @Test
    fun test2() {
        val module = Module()
        print(module)
    }
}
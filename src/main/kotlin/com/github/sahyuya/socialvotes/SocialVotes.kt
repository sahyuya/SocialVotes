package com.github.sahyuya.socialvotes

import com.github.sahyuya.socialvotes.commands.*
import com.github.sahyuya.socialvotes.data.DataManager
import com.github.sahyuya.socialvotes.listeners.*
import org.bukkit.command.*
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Field

class SocialVotes : JavaPlugin() {

    companion object {
        lateinit var instance: SocialVotes
            private set
        lateinit var dataManager: DataManager
            private set
    }

    override fun onLoad() {
        instance = this
    }

    override fun onEnable() {

        dataManager = DataManager(this)
        dataManager.load()

        // --- Commands ---
        registerCommand("socialvotes", SvCommand(), SvTabCompleter())
        registerCommand("sv", SvCommand(), SvTabCompleter())
        registerCommand("svupdate", SvUpdateCommand())
        registerCommand("svtp", TpCommand())

        // --- Listeners ---
        server.pluginManager.registerEvents(SignCreateListener(), this)
        server.pluginManager.registerEvents(SignClickListener(), this)
        server.pluginManager.registerEvents(ProtectEditListener(), this)
        server.pluginManager.registerEvents(ProtectListener(), this)
        server.pluginManager.registerEvents(ChatInput, this)


        logger.info("SocialVotes enabled.")
    }

    override fun onDisable() {
        dataManager.save()
        logger.info("SocialVotes disabled.")
    }

    private fun registerCommand(
        name: String,
        executor: CommandExecutor,
        tabCompleter: TabCompleter? = null
    ) {

        val cmd = object : Command(name), TabCompleter {

            override fun execute(
                sender: CommandSender,
                label: String,
                args: Array<out String>
            ): Boolean {
                return executor.onCommand(sender, this, label, args)
            }

            override fun onTabComplete(
                sender: CommandSender,
                command: Command,
                alias: String,
                args: Array<out String>
            ): MutableList<String> {
                return tabCompleter?.onTabComplete(sender, command, alias, args)?.toMutableList()
                    ?: mutableListOf()
            }
        }

        try {
            val field: Field = server.javaClass.getDeclaredField("commandMap")
            field.isAccessible = true
            val map = field.get(server) as CommandMap
            map.register(name, cmd)
        } catch (ex: Exception) {
            logger.severe("Failed to register command: $name")
        }
    }
}

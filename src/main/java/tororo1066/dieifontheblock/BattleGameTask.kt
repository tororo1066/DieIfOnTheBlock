package tororo1066.dieifontheblock

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import tororo1066.dieifontheblock.data.BlockElement
import tororo1066.tororopluginapi.SJavaPlugin
import tororo1066.tororopluginapi.otherUtils.UsefulUtility
import tororo1066.tororopluginapi.sEvent.SEvent
import tororo1066.tororopluginapi.utils.toPlayer
import java.time.Duration
import java.util.UUID

open class BattleGameTask(val startLocation: Location): Thread() {

    val util = UsefulUtility(SJavaPlugin.plugin)

    val players = ArrayList<UUID>()

    val sEvent = SEvent()
    val safeBlocks = HashMap<UUID, BlockElement>()
    val nextBlock = HashMap<UUID, BlockElement>()
    val bossBars = HashMap<UUID, BossBar>()

    var blockChangeTimeStatic = BattleConfig.BLOCK_CHANGE_TIME
    var blockChangeTime = BattleConfig.BLOCK_CHANGE_TIME
    var changeCount = 0

    val border = startLocation.world.worldBorder
    var borderDirection = BlockFace.NORTH

    val aliveTimeMap = HashMap<UUID, Int>()

    val healProgress = HashMap<UUID, Double>()
    val healBossBars = HashMap<UUID, BossBar>()

    open fun pickNextBlock(){
        if (BattleConfig.BLOCK_SHARE){
            val block = BattleConfig.BLOCK_LIST.filter {
                filter -> filter.name != safeBlocks.values.firstOrNull()?.name
            }.random()
            players.forEach {
                nextBlock[it] = block
            }
        } else {
            players.forEach {
                val block = BattleConfig.BLOCK_LIST.filter {
                    filter -> filter.name != safeBlocks[it]?.name
                }.random()
                nextBlock[it] = block
            }
        }
    }

    fun getDisplayName(block: BlockElement): Component {
        var displayName: Component? = block.displayName?.let { let -> Component.text(let) }
        if (displayName == null){
            val material = Material.getMaterial(block.name)
            displayName = if (material != null) {
                Component.translatable(material.translationKey())
            } else {
                Component.text(block.name)
            }
        }
        return displayName
    }

    fun isSafe(p: Player, location: Location){
        val block = location.clone().subtract(0.0, 1.0, 0.0).block
        if (!block.type.isCollidable) return
        val safeBlock = safeBlocks[p.uniqueId] ?: return
        if ((safeBlock.allowContains && block.type.name.contains(safeBlock.name)) || block.type.name == safeBlock.name){
            util.runTask {
                p.removePotionEffect(PotionEffectType.WITHER)
            }
            return
        }
        util.runTask {
            p.addPotionEffect(PotionEffect(PotionEffectType.WITHER, 1000, 4, false, false, false))
        }
        healProgress[p.uniqueId] = 0.0
    }

    override fun run() {
        BattleConfig.reload()

        if (BattleConfig.BLOCK_LIST.isEmpty()) {
            Bukkit.broadcast(Component.text("§c§lブロックが設定されていません"), Server.BROADCAST_CHANNEL_USERS)
            interrupt()
            return
        }

        players.addAll(Bukkit.getOnlinePlayers().map { it.uniqueId })

        players.forEach {
            aliveTimeMap[it] = 0
            val p = it.toPlayer()!!
            util.runTask { _ ->
                p.teleport(startLocation)
                p.inventory.clear()
                p.inventory.addItem(ItemStack(Material.COMPASS))
                p.gameMode = GameMode.SURVIVAL
                p.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 1000000, 255, false, false, false))
            }
        }

        util.runTask {
            border.size = BattleConfig.BORDER_SIZE
            border.center = startLocation
            border.damageAmount = BattleConfig.BORDER_DAMAGE
            border.damageBuffer = 0.0
            startLocation.world.setGameRule(GameRule.NATURAL_REGENERATION, false)
        }

        for (i in 5 downTo 1){
            players.forEach {
                val p = it.toPlayer()!!
                p.playSound(p.location, Sound.BLOCK_LEVER_CLICK, 2f, 1f)
                p.showTitle(Title.title(Component.text("§e§l-----$i-----"), Component.text(""),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)))
            }
            sleep(1000)
        }

        players.forEach {
            val p = it.toPlayer()!!
            p.showTitle(Title.title(Component.text("§e§lSTART!"), Component.text(""), Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(2))))
            p.playSound(p.location, Sound.ENTITY_WITHER_SPAWN, 1f, 1f)
        }

        sEvent.register(PlayerMoveEvent::class.java) { e ->
            if (e.player.uniqueId !in players) return@register
            if (e.player.gameMode == GameMode.SPECTATOR) return@register

            isSafe(e.player, e.to)
        }

        sEvent.register(PlayerDeathEvent::class.java) { e ->
            if (e.entity.uniqueId !in players) return@register
            val p = e.entity
            p.world.playSound(p.location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
            p.world.spawnParticle(Particle.EXPLOSION_HUGE, p.location, 10, 2.0, 2.0, 2.0)
            Bukkit.broadcast(Component.text("§c§l${p.name}が死亡しました"), Server.BROADCAST_CHANNEL_USERS)

            p.gameMode = GameMode.SPECTATOR
            e.isCancelled = true

            if (players.count { it.toPlayer()?.gameMode != GameMode.SPECTATOR } <= 1) {
                val winner = players.firstOrNull { it.toPlayer()?.gameMode != GameMode.SPECTATOR }?:return@register
                val winnerPlayer = winner.toPlayer()!!

                Bukkit.broadcast(Component.text("§7ブロック変更回数: §a${changeCount}回"), Server.BROADCAST_CHANNEL_USERS)
                Bukkit.broadcast(Component.text("§7勝者: §a${winnerPlayer.name}"), Server.BROADCAST_CHANNEL_USERS)

                players.sortedBy { aliveTimeMap[it] }.forEach {
                    val targetPlayer = it.toPlayer()!!
                    targetPlayer.showTitle(Title.title(Component.text("§d§l${winnerPlayer.name}§e§lの勝利!"), Component.text(""),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(2))))
                    targetPlayer.playSound(targetPlayer.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
                    Bukkit.broadcast(Component.text("§7${targetPlayer.name}の生存時間: §a${(aliveTimeMap[it]!!.toDouble() / 20.0).toInt()}秒"))
                }



                interrupt()
                return@register
            }
        }

        sEvent.register(PlayerQuitEvent::class.java) { e ->
            players.remove(e.player.uniqueId)
        }


        players.forEach {
            nextBlock[it] = BattleConfig.BLOCK_FIRST!!
            val bossBar = BossBar.bossBar(Component.text("§d次のブロック:§a ")
                .append(nextBlock[it]?.let { block -> getDisplayName(block) }?: Component.text("")), 1.0f, BossBar.Color.PINK, BossBar.Overlay.PROGRESS)
            it.toPlayer()?.showBossBar(bossBar)
            bossBars[it] = bossBar

            healProgress[it] = 0.0
            val healBossBar = BossBar.bossBar(Component.text("§a回復ゲージ"), 0.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS)
            it.toPlayer()?.showBossBar(healBossBar)
            healBossBars[it] = healBossBar
        }

        while (true) {

            if (blockChangeTime <= 0){
                changeCount++
                blockChangeTime = BattleConfig.BLOCK_CHANGE_TIME - (changeCount * BattleConfig.BLOCK_CHANGE_TIME_MULTIPLIER).toInt()
                blockChangeTimeStatic = blockChangeTime
                if (blockChangeTime < 0) blockChangeTime = 100

                players.forEach {
                    safeBlocks[it] = nextBlock[it]!!
                    val p = it.toPlayer()?:return@forEach
                    p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.7f)
                    p.showTitle(Title.title(Component.text("§c§l---ブロック変更---"), Component.text("§cボーダーのスピードが上がった"),
                        Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ZERO)))
                    isSafe(p, p.location)
                }

                pickNextBlock()

                players.forEach {
                    bossBars[it]!!.apply {
                        name(Component.text("§d次のブロック:§a ")
                            .append(getDisplayName(nextBlock[it]!!)))
                        progress(1.0f)
                    }
                }

                borderDirection = listOf(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST).random()
            }


            blockChangeTime--
            aliveTimeMap.forEach { (uuid, time) ->
                if (uuid.toPlayer()?.gameMode == GameMode.SPECTATOR) return@forEach
                aliveTimeMap[uuid] = time + 1
            }

            val borderMove = BattleConfig.BORDER_SPEED + (changeCount * BattleConfig.BORDER_SPEED_MULTIPLIER)
            util.runTask {
                when (borderDirection) {
                    BlockFace.NORTH -> {
                        border.center = border.center.add(0.0, 0.0, borderMove)
                    }
                    BlockFace.EAST -> {
                        border.center = border.center.add(borderMove, 0.0, 0.0)
                    }
                    BlockFace.SOUTH -> {
                        border.center = border.center.add(0.0, 0.0, -borderMove)
                    }
                    BlockFace.WEST -> {
                        border.center = border.center.add(-borderMove, 0.0, 0.0)
                    }
                    else -> {}
                }
            }

            players.forEach {
                it.toPlayer()?.sendActionBar(Component.text("§c現在のブロック:§a ")
                    .append(safeBlocks[it]?.let { block -> getDisplayName(block) }?:Component.text("")))
                bossBars[it]!!.progress((blockChangeTime.toDouble() / blockChangeTimeStatic.toDouble()).toFloat())
                healProgress[it] = healProgress[it]!! + BattleConfig.HEAL_SPEED
                if (healProgress[it]!! >= 1.0){
                    val p = it.toPlayer()?:return@forEach
                    p.health = p.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.value
                    healProgress[it] = 0.0
                    p.playSound(p.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                }
                it.toPlayer()?.compassTarget = border.center
                if (it.toPlayer()?.gameMode == GameMode.SPECTATOR) return@forEach
                healBossBars[it]!!.progress(healProgress[it]!!.toFloat())
            }

            for (i in 1..255){
                startLocation.world.spawnParticle(Particle.REDSTONE, border.center.clone().add(0.0, i.toDouble(), 0.0), 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.WHITE, 1.5f))
                startLocation.world.spawnParticle(Particle.REDSTONE, border.center.clone().add(0.0, i.toDouble()-0.5, 0.0), 1, 0.0, 0.0, 0.0, 0.0, Particle.DustOptions(Color.WHITE, 1.5f))
            }

            when(blockChangeTime){
                60 -> {
                    players.forEach {
                        it.toPlayer()?.playSound(it.toPlayer()!!.location, Sound.BLOCK_LEVER_CLICK, 2f, 1.5f)
                        it.toPlayer()?.showTitle(Title.title(Component.text("§e§l-----3-----"), Component.text(""),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)))
                    }
                }
                40 -> {
                    players.forEach {
                        it.toPlayer()?.playSound(it.toPlayer()!!.location, Sound.BLOCK_LEVER_CLICK, 2f, 1.5f)
                        it.toPlayer()?.showTitle(Title.title(Component.text("§e§l-----2-----"), Component.text(""),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)))
                    }
                }
                20 -> {
                    players.forEach {
                        it.toPlayer()?.playSound(it.toPlayer()!!.location, Sound.BLOCK_LEVER_CLICK, 2f, 1.5f)
                        it.toPlayer()?.showTitle(Title.title(Component.text("§e§l-----1-----"), Component.text(""),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO)))
                    }
                }
            }

            sleep(50)
        }
    }

    override fun interrupt() {
        sEvent.unregisterAll()
        players.forEach {
            val p = it.toPlayer()?:return@forEach
            p.hideBossBar(bossBars[it]!!)
            p.hideBossBar(healBossBars[it]!!)
            p.removePotionEffect(PotionEffectType.WITHER)
            p.removePotionEffect(PotionEffectType.SATURATION)
        }
        util.runTask {
            border.reset()
        }
        super.interrupt()
    }
}
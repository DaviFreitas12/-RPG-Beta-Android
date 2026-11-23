package com.example.rpgsombrio.ui.avatar

import com.example.rpgsombrio.data.CharacterLook

/**
 * Gerador do avatar em pixel art (corpo inteiro, 32x44). Tudo é desenhado por código em uma grade de pixels:
 * depois das camadas, um passe automático aplica luz/sombra nas bordas de cada peça e o contorno escuro.
 */
internal const val SPRITE_W = 32
internal const val SPRITE_H = 44
private const val DETAIL = -1

private fun rgb(r: Int, g: Int, b: Int) = (255 shl 24) or (r shl 16) or (g shl 8) or b
private fun mixC(a: Int, b: Int, f: Float): Int {
    fun ch(sh: Int): Int {
        val x = (a shr sh) and 255
        val y = (b shr sh) and 255
        return (x + (y - x) * f).toInt().coerceIn(0, 255)
    }
    return rgb(ch(16), ch(8), ch(0))
}
private fun darkC(c: Int, f: Float) = mixC(c, rgb(0, 0, 0), f)
private fun lightC(c: Int, f: Float) = mixC(c, rgb(255, 255, 255), f)

internal val SKIN_PALETTE = listOf(
    rgb(246, 217, 196), rgb(232, 184, 150), rgb(198, 134, 66), rgb(141, 85, 36), rgb(92, 58, 33),
    rgb(124, 168, 96), rgb(214, 206, 236), rgb(190, 70, 70), rgb(96, 132, 190), rgb(150, 152, 160)
)
internal val HAIR_PALETTE = listOf(
    rgb(27, 27, 31), rgb(74, 47, 27), rgb(181, 101, 29), rgb(227, 193, 111), rgb(201, 204, 214),
    rgb(244, 244, 244), rgb(91, 58, 140), rgb(46, 111, 94), rgb(160, 40, 40), rgb(60, 120, 200), rgb(120, 130, 145), rgb(190, 150, 110), rgb(35, 75, 55), rgb(210, 80, 120)
)
internal val EYE_PALETTE = listOf(
    rgb(59, 42, 26), rgb(46, 111, 208), rgb(46, 139, 87), rgb(138, 143, 153),
    rgb(212, 160, 23), rgb(192, 57, 43), rgb(150, 60, 200)
)
internal val CLOTH_PALETTE = listOf(
    rgb(122, 31, 43), rgb(45, 74, 122), rgb(47, 93, 58), rgb(58, 58, 68),
    rgb(107, 78, 46), rgb(91, 58, 140), rgb(184, 137, 43), rgb(200, 200, 210), rgb(36, 91, 91), rgb(218, 207, 177), rgb(35, 39, 58), rgb(92, 105, 54), rgb(115, 48, 65)
)
private val STEEL = rgb(168, 176, 194)
private val GOLD = rgb(224, 184, 72)
private val LEATHER = rgb(110, 76, 44)
private val OUTLINE = rgb(26, 20, 34)
private val WHITE = rgb(245, 245, 250)

/** Largura da cabeça, do tronco e comprimento das pernas por raça (humano, elfo, anão, orc, tiefling, halfling). */
private val RACE_DIMS = listOf(Triple(10, 10, 9), Triple(10, 8, 10), Triple(12, 12, 6), Triple(12, 12, 9), Triple(10, 10, 9), Triple(10, 8, 5))

private class PixelGrid {
    val c = IntArray(SPRITE_W * SPRITE_H)
    val m = IntArray(SPRITE_W * SPRITE_H)
    private var n = 0
    fun newId(): Int { n += 1; return n }
    fun put(x: Int, y: Int, col: Int, mat: Int) {
        if (x in 0 until SPRITE_W && y in 0 until SPRITE_H) { c[y * SPRITE_W + x] = col; m[y * SPRITE_W + x] = mat }
    }
    fun matAt(x: Int, y: Int): Int = m[y * SPRITE_W + x]
    /** Retângulo; x1 e y1 são exclusivos. Cada chamada vira uma "peça" própria (para luz e sombra). */
    fun rect(x0: Int, y0: Int, x1: Int, y1: Int, col: Int, mat: Int = newId()) {
        for (y in y0 until y1) for (x in x0 until x1) put(x, y, col, mat)
    }
    fun ell(cx: Float, cy: Float, rx: Float, ry: Float, col: Int, mat: Int = newId()) {
        for (y in (cy - ry).toInt() - 1..(cy + ry).toInt() + 1) for (x in (cx - rx).toInt() - 1..(cx + rx).toInt() + 1) {
            val dx = (x + 0.5f - cx) / rx
            val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f) put(x, y, col, mat)
        }
    }
    fun line(xa: Int, ya: Int, xb: Int, yb: Int, col: Int, mat: Int = newId()) {
        var x = xa; var y = ya
        val dx = kotlin.math.abs(xb - xa); val dy = kotlin.math.abs(yb - ya)
        val sx = if (xa < xb) 1 else -1; val sy = if (ya < yb) 1 else -1
        var e = dx - dy
        while (true) {
            put(x, y, col, mat)
            if (x == xb && y == yb) break
            val e2 = 2 * e
            if (e2 > -dy) { e -= dy; x += sx }
            if (e2 < dx) { e += dx; y += sy }
        }
    }
}

private fun <T> List<T>.pick(i: Int): T = this[i.coerceIn(0, lastIndex)]

internal fun buildSprite(L: CharacterLook): IntArray {
    val g = PixelGrid()
    val cx = 16
    val skin = SKIN_PALETTE.pick(L.skin)
    val hair = HAIR_PALETTE.pick(L.hairColor)
    val hairD = darkC(hair, .35f)
    val eye = EYE_PALETTE.pick(L.eyeColor)
    val cloth = CLOTH_PALETTE.pick(L.outfitColor)
    val (hw, tw0, leg) = RACE_DIMS.pick(L.race)
    val tw = maxOf(6, tw0 + listOf(0, -2, 2).pick(L.body))
    val boots = 3; val torsoH = 11; val headH = 11
    val top = 42 - boots - leg - torsoH - 1 - headH
    val y0 = top; val y1 = top + headH
    val ty = y1 + 1; val tb = ty + torsoH
    val lb = tb + leg
    val x0 = cx - hw / 2; val x1 = cx + hw / 2
    val tx0 = cx - tw / 2; val tx1 = cx + tw / 2
    val pants = rgb(62, 58, 76); val bootc = rgb(72, 50, 34)
    val o = L.outfit
    val heavy = o in setOf(1, 5, 6, 10, 11, 12, 13, 14, 20, 21, 24)
    val hs = L.hairStyle

    // --- camadas de trás: cauda, capa, cabelo comprido, capuz
    if (L.race == 4) {
        val m = g.newId(); val tc = darkC(skin, .1f)
        for ((a, b) in listOf(Pair(tx1 - 1, tb - 1), Pair(tx1 + 1, tb + 1), Pair(tx1 + 2, tb + 3), Pair(tx1 + 3, tb + 5), Pair(tx1 + 3, tb + 7), Pair(tx1 + 4, tb + 8))) {
            g.put(a, b, tc, m); g.put(a + 1, b, tc, m)
        }
        g.rect(tx1 + 3, tb + 8, tx1 + 6, tb + 10, rgb(150, 40, 50))
    }
    if (L.cape > 0) {
        val ln = if (L.cape == 1) tb + 4 else lb - 1
        g.rect(tx0 - 2, ty, tx1 + 2, ln, darkC(cloth, .15f))
    }
    if (L.cape == 4) {
        g.put(tx0 - 2, tb + 2, 0, 0); g.put(tx1 + 1, tb + 5, 0, 0)
        g.put(tx0 - 1, lb - 2, 0, 0)
    }
    if (L.cape == 5) {
        g.rect(tx0 - 2, ty, tx1 + 2, ty + 3, darkC(cloth, .12f))
    }
    if (hs == 2) g.rect(x0 - 1, y0 + 2, x1 + 1, ty + 8, hair)
    if (hs == 5) g.rect(x1, y0 + 2, x1 + 2, y0 + 14, hair)
    if (hs == 6) {
        val m = g.newId()
        for ((i, yy) in (y0 + 3 until y0 + 17).withIndex()) g.rect(x0 - 2, yy, x0, yy + 1, if (i % 2 == 0) hair else hairD, m)
    }
    if (hs == 9 || hs == 14) {
        val m = g.newId()
        for (yy in y0 + 2 until ty + 8) {
            val shift = if ((yy / 2) % 2 == 0) 0 else 1
            g.rect(x0 - 1 + shift, yy, x0 + shift, yy + 1, if (yy % 3 == 0) hairD else hair, m)
            g.rect(x1 - 1 - shift, yy, x1 - shift, yy + 1, if (yy % 3 == 0) hairD else hair, m)
        }
    }
    if (hs == 11 || hs == 12) {
        val m = g.newId()
        val strands = if (hs == 11) listOf(x0 - 2, x0, x1 - 1, x1 + 1) else listOf(x0 - 2, x1 + 1)
        strands.forEachIndexed { i, xx ->
            for (yy in y0 + 3 until ty + 8 + (i % 2)) g.put(xx + ((yy / 3 + i) % 2), yy, if (yy % 4 == 0) hairD else hair, m)
        }
    }
    if (hs == 10) {
        g.ell(cx.toFloat(), (y0 + 1).toFloat(), hw / 2f + 3f, 4.5f, hair)
        g.ell((cx - 4).toFloat(), (y0 + 3).toFloat(), 2.5f, 3f, hair)
        g.ell((cx + 4).toFloat(), (y0 + 3).toFloat(), 2.5f, 3f, hair)
    }
    if (hs == 16) { // raspado lateral com volume no topo
        g.rect(x0 - 1, y0 + 1, x0 + 1, ty + 3, hairD)
        g.rect(x1 - 1, y0 + 1, x1 + 1, ty + 3, hairD)
        g.rect(cx - 3, y0 - 2, cx + 3, y0 + 2, hair)
    }
    if (hs == 17) { // trança lateral longa
        val braid = g.newId()
        for (yy in y0 + 3 until ty + 9) {
            val xx = if ((yy / 2) % 2 == 0) x1 + 1 else x1 + 2
            g.put(xx, yy, if (yy % 3 == 0) hairD else hair, braid)
        }
    }
    if (hs == 18) { // topete alto penteado para trás
        g.rect(cx - 2, y0 - 3, cx + 3, y0, hair)
        g.rect(cx - 1, y0 - 4, cx + 2, y0 - 2, hairD)
    }
    if (hs == 19) { // dois coques laterais
        g.ell((cx - 4).toFloat(), (y0 - 1).toFloat(), 2.3f, 2.5f, hair)
        g.ell((cx + 4).toFloat(), (y0 - 1).toFloat(), 2.3f, 2.5f, hair)
    }
    if (hs == 23) { // mullet: nuca alongada e laterais curtas
        g.rect(x0 - 1, y0 + 3, x0 + 1, ty + 11, hair)
        g.rect(x1 - 1, y0 + 3, x1 + 1, ty + 11, hair)
        val mullet = g.newId()
        for (yy in ty + 5 until minOf(SPRITE_H, ty + 12)) g.put(cx + ((yy / 2) % 2), yy, if (yy % 3 == 0) hairD else hair, mullet)
    }
    if (hs == 29) { // cabelo longo preso atrás
        g.rect(x0 - 1, y0 + 2, x0 + 1, ty + 8, hair)
        g.rect(x1 - 1, y0 + 2, x1 + 1, ty + 8, hair)
        g.rect(cx - 1, y0 + 1, cx + 1, ty + 13, hairD)
    }
    if (L.headgear == 1) g.ell(cx.toFloat(), (y0 + 5).toFloat(), hw / 2f + 1.5f, 7f, darkC(cloth, .2f))

    // --- pernas e botas
    val lw = maxOf(2, tw / 2 - 1)
    for (sd in listOf(-1, 1)) {
        val lx = if (sd < 0) cx - 1 - lw else cx + 1
        g.rect(lx, tb, lx + lw, lb, pants)
        g.rect(lx - (if (sd > 0) 0 else 1), lb, lx + lw + (if (sd > 0) 1 else 0), lb + boots, if (heavy) STEEL else bootc)
    }

    // --- tronco, pescoço, braços e mãos
    val body = when (o) { 1, 5, 6, 10, 11, 12, 13, 14, 20, 21, 24 -> STEEL; 3, 7, 9, 18, 25 -> LEATHER; else -> cloth }
    val sleeve = if (heavy) STEEL else cloth
    when (o) {
        2 -> {
            g.rect(tx0, ty, tx1, lb + 1, body)
            g.rect(tx0 - 1, tb + 3, tx1 + 1, lb + 1, body)
            g.rect(tx0 - 1, lb, tx1 + 1, lb + 1, GOLD)
        }
        0 -> g.rect(tx0, ty, tx1, tb + 2, body)
        else -> g.rect(tx0, ty, tx1, tb, body)
    }
    g.rect(cx - 1, y1 - 1, cx + 1, ty + 1, darkC(skin, .18f))
    for (sd in listOf(-1, 1)) {
        val ax = if (sd < 0) tx0 - 2 else tx1
        val padL = if (sd < 0) 1 else 0
        val padR = if (sd < 0) 0 else 1
        g.rect(ax, ty, ax + 2, ty + 7, if (o == 3 || o == 18) cloth else sleeve)
        g.rect(ax, ty + 7, ax + 2, ty + 10, skin)
        if (o == 2) g.rect(ax - padL, ty + 5, ax + 2 + padR, ty + 8, sleeve)
        if (heavy) g.rect(ax - padL, ty, ax + 2 + padR, ty + 3, lightC(STEEL, .12f))
    }
    when (o) {
        0 -> {
            g.rect(tx0, ty + 7, tx1, ty + 8, LEATHER, DETAIL); g.put(cx, ty + 7, GOLD, DETAIL)
            g.put(cx - 1, ty, skin, DETAIL); g.put(cx, ty, skin, DETAIL); g.put(cx, ty + 1, skin, DETAIL)
        }
        1 -> {
            g.rect(cx - 1, ty + 1, cx + 1, tb, cloth, DETAIL)
            g.rect(tx0, ty + 7, tx1, ty + 8, darkC(STEEL, .35f), DETAIL)
            g.rect(tx0, ty + 3, tx1, ty + 4, darkC(STEEL, .2f), DETAIL)
        }
        2 -> {
            g.rect(cx - 1, ty, cx + 1, lb, GOLD, DETAIL); g.rect(tx0, ty + 6, tx1, ty + 7, darkC(cloth, .4f), DETAIL)
            g.put(cx - 1, ty, skin, DETAIL); g.put(cx, ty, skin, DETAIL)
        }
        3 -> {
            g.line(tx0, ty, tx1 - 1, tb - 2, darkC(LEATHER, .45f), DETAIL)
            g.line(tx1 - 1, ty, tx0, tb - 2, darkC(LEATHER, .45f), DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, darkC(LEATHER, .5f), DETAIL)
            g.put(cx - 1, ty, skin, DETAIL); g.put(cx, ty, skin, DETAIL)
        }
    }
    // Modelos adicionais de vestuário: silhuetas e acabamentos próprios.
    when (o) {
        4 -> { // manto com gola e bordas largas
            g.rect(tx0 - 1, ty + 2, tx0 + 1, tb + 2, darkC(cloth, .18f), DETAIL)
            g.rect(tx1 - 1, ty + 2, tx1 + 1, tb + 2, lightC(cloth, .12f), DETAIL)
            g.rect(cx - 1, ty, cx + 1, tb + 1, GOLD, DETAIL)
        }
        5 -> { // cota de malha
            for (yy in ty + 2 until tb) for (xx in tx0 until tx1) if ((xx + yy) % 2 == 0) g.put(xx, yy, darkC(STEEL, .18f), DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, STEEL, DETAIL)
        }
        6 -> { // armadura real
            g.rect(tx0, ty + 1, tx1, ty + 3, GOLD, DETAIL)
            g.rect(cx - 2, ty + 3, cx + 2, tb - 1, darkC(STEEL, .25f), DETAIL)
            g.put(cx, ty + 5, rgb(170, 40, 55), DETAIL)
        }
        7 -> { // patrulheiro: couro, fivelas e faixa
            g.line(tx0, ty + 1, tx1 - 1, tb - 1, darkC(LEATHER, .4f), DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, rgb(74, 91, 48), DETAIL)
            g.put(cx, tb - 3, GOLD, DETAIL)
        }
        8 -> { // traje arcano
            g.rect(cx - 1, ty + 2, cx + 1, tb - 1, GOLD, DETAIL)
            g.put(cx, ty + 5, rgb(100, 170, 220), DETAIL)
            g.put(cx - 2, ty + 7, rgb(100, 170, 220), DETAIL); g.put(cx + 2, ty + 7, rgb(100, 170, 220), DETAIL)
        }
        9 -> { // viajante
            g.rect(tx0, tb - 3, tx1, tb - 2, LEATHER, DETAIL)
            g.rect(tx1 - 2, ty + 2, tx1, ty + 6, darkC(cloth, .3f), DETAIL)
        }
        10 -> { // armadura completa de placas
            g.rect(tx0 - 2, ty, tx0 + 1, ty + 4, STEEL, DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 2, ty + 4, STEEL, DETAIL)
            g.rect(tx0, ty + 2, tx1, tb - 1, lightC(STEEL, .08f), DETAIL)
            g.rect(cx - 2, ty + 3, cx + 2, tb - 2, darkC(STEEL, .28f), DETAIL)
            g.rect(tx0 - 1, tb - 1, tx1 + 1, tb + 1, GOLD, DETAIL)
            g.put(cx, ty + 5, GOLD, DETAIL)
        }
        11 -> { // armadura de escamas sobre couro
            for (yy in ty + 1 until tb) for (xx in tx0 until tx1) {
                if ((xx + yy) % 2 == 0) g.put(xx, yy, lightC(STEEL, .05f), DETAIL)
                else if ((xx + yy) % 3 == 0) g.put(xx, yy, darkC(STEEL, .2f), DETAIL)
            }
            g.rect(tx0 - 1, ty + 1, tx0 + 1, ty + 5, LEATHER, DETAIL)
            g.rect(tx1 - 1, ty + 1, tx1 + 1, ty + 5, LEATHER, DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, LEATHER, DETAIL)
        }
        12 -> { // brigandina: placas rebitadas em tecido grosso
            g.rect(tx0, ty + 1, tx1, tb - 1, darkC(LEATHER, .12f), DETAIL)
            for (yy in ty + 2 until tb - 1 step 2) for (xx in tx0 + 1 until tx1 step 2)
                g.put(xx, yy, GOLD, DETAIL)
            g.rect(cx - 1, ty + 1, cx + 1, tb - 2, STEEL, DETAIL)
            g.rect(tx0 - 1, ty, tx0 + 1, ty + 3, STEEL, DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 1, ty + 3, STEEL, DETAIL)
        }
        13 -> { // cavaleiro negro: placas escuras e ombreiras angulares
            val blackSteel = rgb(48, 51, 62)
            g.rect(tx0 - 2, ty, tx0 + 2, ty + 4, blackSteel, DETAIL)
            g.rect(tx1 - 2, ty, tx1 + 2, ty + 4, blackSteel, DETAIL)
            g.rect(tx0, ty + 1, tx1, tb - 1, blackSteel, DETAIL)
            g.rect(cx - 1, ty + 2, cx + 1, tb - 2, rgb(115, 35, 48), DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, rgb(145, 38, 48), DETAIL)
            g.put(cx, ty + 5, GOLD, DETAIL)
        }
        14 -> { // guarda real: aço claro, ombreiras douradas e faixa
            g.rect(tx0 - 2, ty, tx0 + 1, ty + 3, GOLD, DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 2, ty + 3, GOLD, DETAIL)
            g.rect(tx0, ty + 1, tx1, tb - 1, lightC(STEEL, .16f), DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, rgb(150, 35, 48), DETAIL)
            g.rect(cx - 1, ty + 2, cx + 1, tb - 2, GOLD, DETAIL)
        }
        15 -> { // guerreiro tribal: couro, peles e faixa de tecido
            g.rect(tx0 - 1, ty, tx0 + 2, ty + 5, LEATHER, DETAIL)
            g.rect(tx1 - 2, ty, tx1 + 1, ty + 5, LEATHER, DETAIL)
            g.rect(tx0, ty + 1, tx1, tb - 1, darkC(LEATHER, .08f), DETAIL)
            g.rect(tx0 - 1, ty, tx1 + 1, ty + 1, rgb(190, 175, 145), DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, rgb(125, 48, 42), DETAIL)
            g.line(tx0 + 1, ty + 2, tx1 - 1, tb - 3, GOLD, DETAIL)
        }
        16 -> { // necromante: manto sombrio com gola alta e sigilo
            g.rect(tx0 - 2, ty, tx0 + 1, tb + 2, darkC(cloth, .38f), DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 2, tb + 2, darkC(cloth, .38f), DETAIL)
            g.rect(cx - 2, ty - 1, cx + 2, ty + 3, rgb(48, 42, 66), DETAIL)
            g.rect(cx - 1, ty + 3, cx + 1, tb, rgb(105, 65, 135), DETAIL)
            g.put(cx, ty + 5, rgb(100, 205, 190), DETAIL)
            g.rect(tx0 - 1, tb + 1, tx1 + 1, tb + 2, rgb(65, 48, 82), DETAIL)
        }
        17 -> { // clérigo: túnica clara, estola e símbolo sagrado
            g.rect(tx0 - 1, ty + 1, tx0 + 1, tb + 1, lightC(cloth, .24f), DETAIL)
            g.rect(tx1 - 1, ty + 1, tx1 + 1, tb + 1, lightC(cloth, .24f), DETAIL)
            g.line(cx - 2, ty + 1, cx + 1, tb - 2, GOLD, DETAIL)
            g.line(cx + 2, ty + 1, cx - 1, tb - 2, GOLD, DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, GOLD, DETAIL)
            g.put(cx, ty + 4, GOLD, DETAIL)
        }
        18 -> { // assassino: couro ajustado, capuz visual e faixas
            g.rect(tx0 - 1, ty, tx0 + 1, tb, darkC(LEATHER, .25f), DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 1, tb, darkC(LEATHER, .25f), DETAIL)
            g.line(tx0 + 1, ty + 1, tx1 - 1, tb - 1, rgb(95, 38, 48), DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, rgb(95, 38, 48), DETAIL)
            g.put(cx, tb - 3, GOLD, DETAIL)
        }
        19 -> { // nobre: casaco longo, gola e acabamento bordado
            g.rect(tx0 - 1, ty, tx0 + 1, tb + 2, cloth, DETAIL)
            g.rect(tx1 - 1, ty, tx1 + 1, tb + 2, cloth, DETAIL)
            g.rect(cx - 1, ty, cx + 1, tb + 1, GOLD, DETAIL)
            g.rect(tx0, ty + 2, tx0 + 1, tb, lightC(cloth, .24f), DETAIL)
            g.rect(tx1 - 1, ty + 2, tx1, tb, lightC(cloth, .24f), DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, GOLD, DETAIL)
            g.put(cx, ty + 5, rgb(150, 35, 48), DETAIL)
        }
        20 -> { // armadura rúnica: placas gravadas e núcleo mágico
            g.rect(tx0 - 1, ty, tx0 + 2, ty + 4, STEEL, DETAIL)
            g.rect(tx1 - 2, ty, tx1 + 1, ty + 4, STEEL, DETAIL)
            g.rect(tx0, ty + 1, tx1, tb - 1, darkC(STEEL, .12f), DETAIL)
            g.line(cx - 2, ty + 2, cx + 1, tb - 3, rgb(90, 190, 210), DETAIL)
            g.line(cx + 2, ty + 2, cx - 1, tb - 3, rgb(90, 190, 210), DETAIL)
            g.put(cx, ty + 5, rgb(100, 220, 230), DETAIL)
        }
        21 -> { // couraça de gladiador: peito reforçado e proteção assimétrica
            g.rect(tx0, ty + 1, tx1, tb - 1, rgb(145, 65, 48), DETAIL)
            g.rect(tx0 - 2, ty, tx0 + 2, ty + 5, STEEL, DETAIL)
            g.rect(tx1 - 1, ty + 1, tx1 + 1, ty + 3, GOLD, DETAIL)
            g.line(tx0 + 1, ty + 2, tx1 - 1, tb - 2, GOLD, DETAIL)
            g.rect(tx0, tb - 2, tx1, tb - 1, STEEL, DETAIL)
        }
        22 -> { // manto de viajante: sobreposição, gola e bolsa lateral
            g.rect(tx0 - 1, ty, tx0 + 1, tb + 2, darkC(cloth, .2f), DETAIL)
            g.rect(tx1 - 1, ty + 2, tx1 + 1, tb + 2, lightC(cloth, .08f), DETAIL)
            g.line(tx0 + 1, ty + 1, cx + 1, tb, LEATHER, DETAIL)
            g.rect(tx1 - 1, tb - 3, tx1 + 2, tb, LEATHER, DETAIL)
            g.put(tx1, tb - 2, GOLD, DETAIL)
        }
        23 -> { // vestes druídicas: folhas e amarrações naturais
            g.rect(tx0 - 1, ty + 1, tx0 + 1, tb, rgb(65, 100, 58), DETAIL)
            g.rect(tx1 - 1, ty + 1, tx1 + 1, tb, rgb(65, 100, 58), DETAIL)
            g.line(cx - 2, ty + 2, cx + 1, tb - 2, rgb(130, 155, 75), DETAIL)
            g.put(cx - 1, ty + 4, rgb(155, 175, 85), DETAIL)
            g.put(cx + 1, ty + 7, rgb(155, 175, 85), DETAIL)
        }
        24 -> { // armadura de sentinela: placas largas e faixa de identificação
            g.rect(tx0 - 2, ty, tx0 + 2, ty + 4, lightC(STEEL, .12f), DETAIL)
            g.rect(tx1 - 2, ty, tx1 + 2, ty + 4, lightC(STEEL, .12f), DETAIL)
            g.rect(tx0, ty + 1, tx1, tb - 1, STEEL, DETAIL)
            g.rect(tx0, tb - 3, tx1, tb - 2, rgb(45, 85, 125), DETAIL)
            g.rect(cx - 1, ty + 2, cx + 1, tb - 3, GOLD, DETAIL)
        }
        25 -> { // alquimista: avental, tiras e frascos coloridos
            g.rect(tx0 - 1, ty + 1, tx1 + 1, tb, darkC(cloth, .12f), DETAIL)
            g.rect(cx - 2, ty + 3, cx + 2, tb + 1, rgb(190, 185, 160), DETAIL)
            g.line(tx0, ty + 1, tx1, tb - 2, LEATHER, DETAIL)
            g.put(tx1, ty + 5, rgb(70, 175, 195), DETAIL)
            g.put(tx1, ty + 7, rgb(175, 90, 65), DETAIL)
            g.rect(cx - 1, tb - 2, cx + 1, tb - 1, GOLD, DETAIL)
        }
    }
    if (L.cape > 0) g.rect(cx - 2, ty, cx + 2, ty + 1, GOLD, DETAIL)

    // --- cabeça
    val headMat = g.newId()
    g.rect(x0, y0 + 1, x1, y1, skin, headMat)
    for (x in x0 + 1 until x1 - 1) g.put(x, y0, skin, headMat)
    g.put(x0, y0 + 1, 0, 0); g.put(x1 - 1, y0 + 1, 0, 0)
    g.put(x0, y1 - 1, 0, 0); g.put(x1 - 1, y1 - 1, 0, 0)
    if (L.face == 2) {
        for (yy in listOf(y1 - 2, y1 - 1)) {
            g.put(x0, yy, 0, 0); g.put(x1 - 1, yy, 0, 0)
            if (yy == y1 - 1) { g.put(x0 + 1, yy, 0, 0); g.put(x1 - 2, yy, 0, 0) }
        }
    }
    if (L.face == 1) { g.put(x0, y1 - 2, skin, headMat); g.put(x1 - 1, y1 - 2, skin, headMat) }
    // orelhas (pontudas em elfo, orc e halfling)
    val em = g.newId(); val ec = darkC(skin, .06f)
    g.put(x0 - 1, y0 + 5, ec, em); g.put(x0 - 1, y0 + 6, ec, em); g.put(x1, y0 + 5, ec, em); g.put(x1, y0 + 6, ec, em)
    if (L.race == 1 || L.race == 3 || L.race == 5) {
        val n = if (L.race == 1) 2 else 1
        for (k in 0..n) {
            val extra = if (k > 0) 1 else 0
            g.put(x0 - 2 - extra, y0 + 4 - k, ec, em); g.put(x1 + 1 + extra, y0 + 4 - k, ec, em)
        }
        g.put(x0 - 1, y0 + 4, ec, em); g.put(x1, y0 + 4, ec, em)
    }
    // olhos, sobrancelhas, nariz
    val ey = y0 + 5; val ex1 = cx - 3; val ex2 = cx + 2
    for (ex in listOf(ex1, ex2)) {
        when (L.eyeStyle) {
            0 -> {
                g.put(ex, ey, eye, DETAIL); g.put(ex, ey + 1, darkC(eye, .55f), DETAIL)
                g.put(ex + 1, ey, WHITE, DETAIL); g.put(ex + 1, ey + 1, WHITE, DETAIL)
            }
            1 -> { g.put(ex, ey + 1, darkC(eye, .5f), DETAIL); g.put(ex + 1, ey + 1, eye, DETAIL) }
            else -> {
                g.put(ex, ey, WHITE, DETAIL); g.put(ex + 1, ey, eye, DETAIL)
                g.put(ex, ey + 1, eye, DETAIL); g.put(ex + 1, ey + 1, darkC(eye, .6f), DETAIL)
            }
        }
        g.put(ex, ey - 2, hairD, DETAIL); g.put(ex + 1, ey - 2, hairD, DETAIL)
    }
    g.put(cx - 1, ey + 3, darkC(skin, .25f), DETAIL)
    // barba
    val bd = L.beard; val jaw = y0 + 7
    if (bd == 1) {
        val c2 = mixC(skin, hair, .35f)
        for (yy in jaw + 1 until y1) for (xx in x0 until x1) {
            if ((xx + yy) % 2 == 0 && !(yy == y1 - 1 && (xx == x0 || xx == x1 - 1))) g.put(xx, yy, c2, DETAIL)
        }
    }
    if (bd == 2 || bd == 4) {
        g.rect(x0, jaw, x1, y1 - 1, hair); g.rect(x0 + 1, y1 - 1, x1 - 1, y1, hair)
        if (bd == 4) for (k in 0 until 6) g.rect(x0 + 1 + k / 2, y1 + k, x1 - 1 - k / 2, y1 + k + 1, hair)
        g.rect(cx - 2, jaw + 1, cx + 2, jaw + 2, skin, DETAIL)
    }
    if (bd == 3) g.rect(cx - 3, y0 + 8, cx + 3, y0 + 9, hair)
    if (bd == 5) { g.rect(cx - 2, y1 - 3, cx + 2, y1, hair); g.rect(cx - 1, y1, cx + 1, y1 + 2, hair) }
    if (bd == 6) {
        g.rect(x0 + 1, jaw + 1, x0 + 3, y1 - 1, hair)
        g.rect(x1 - 3, jaw + 1, x1 - 1, y1 - 1, hair)
        g.rect(cx - 2, y1 - 1, cx + 2, y1, hair)
    }
    if (bd == 7) {
        for (yy in jaw + 1 until y1) for (xx in x0 + 1 until x1 - 1) if ((xx * 3 + yy) % 3 != 0) g.put(xx, yy, hair, DETAIL)
    }
    if (bd == 8) {
        g.rect(cx - 3, y0 + 8, cx + 3, y0 + 9, hair)
        g.rect(cx - 2, y1 - 2, cx + 2, y1, hair)
        g.rect(cx - 1, y1, cx + 1, y1 + 1, hair)
    }
    // boca (e presas do orc)
    val my = y0 + 8; val mc = rgb(140, 50, 56)
    when (L.mouth) {
        0 -> { g.put(cx - 1, my, mc, DETAIL); g.put(cx, my, mc, DETAIL) }
        1 -> { g.put(cx - 2, my, mc, DETAIL); g.put(cx - 1, my + 1, mc, DETAIL); g.put(cx, my + 1, mc, DETAIL); g.put(cx + 1, my, mc, DETAIL) }
        2 -> for (k in -2 until 2) g.put(cx + k, my, darkC(mc, .3f), DETAIL)
        else -> {
            g.put(cx - 1, my, mc, DETAIL); g.put(cx, my, mc, DETAIL)
            g.put(cx - 1, my + 1, WHITE, DETAIL); g.put(cx, my + 1, WHITE, DETAIL)
        }
    }
    if (L.race == 3) {
        val tusk = rgb(240, 236, 215)
        g.put(cx - 3, my, tusk, DETAIL); g.put(cx - 3, my - 1, tusk, DETAIL)
        g.put(cx + 2, my, tusk, DETAIL); g.put(cx + 2, my - 1, tusk, DETAIL)
    }
    // cabelo da frente
    if (hs != 0) {
        val hm = g.newId()
        g.rect(x0, y0 - 1, x1, y0 + 3, hair, hm)
        g.put(x0, y0 - 1, 0, 0); g.put(x1 - 1, y0 - 1, 0, 0)
        for (xx in x0 until x1) if ((xx - x0) % 3 == 1) g.put(xx, y0 + 3, hair, hm)
        g.rect(x0, y0 + 3, x0 + 1, y0 + 6, hair, hm); g.rect(x1 - 1, y0 + 3, x1, y0 + 6, hair, hm)
        if (hs == 3) g.ell(cx.toFloat(), (y0 - 3).toFloat(), 2.5f, 2.5f, hair)
        if (hs == 4) {
            g.rect(x0, y0 - 1, x1, y0 + 3, 0, 0)
            g.rect(x0, y0 + 3, x0 + 1, y0 + 6, 0, 0); g.rect(x1 - 1, y0 + 3, x1, y0 + 6, 0, 0)
            g.rect(cx - 1, y0 - 5, cx + 1, y0 + 3, hair)
            g.rect(x0, y0, x1, y0 + 2, mixC(skin, hair, .3f), DETAIL)
        }
        if (hs == 7) for (xx in x0 until x1 step 2) {
            g.put(xx, y0 - 2, hair, hm)
            if (xx % 4 == 0) g.put(xx, y0 - 3, hair, hm)
        }
        if (hs == 8 || hs == 14) {
            g.rect(cx - 3, y0 + 1, cx + 2, y0 + 4, hair, hm)
            g.put(cx - 4, y0 + 3, hairD, hm)
        }
        if (hs == 9 || hs == 15) {
            g.put(x0 + 2, y0 - 2, hair, hm); g.put(cx - 1, y0 - 3, hair, hm)
            g.put(cx + 2, y0 - 2, hairD, hm); g.put(x1 - 2, y0 - 1, hair, hm)
        }
        if (hs == 10) {
            g.ell(cx.toFloat(), (y0 - 1).toFloat(), hw / 2f + 3f, 3f, hair, hm)
        }
        if (hs == 11 || hs == 12) {
            g.rect(x0, y0 + 2, x0 + 2, y0 + 5, hairD, hm)
            g.rect(x1 - 2, y0 + 2, x1, y0 + 5, hairD, hm)
        }
        if (hs == 13) {
            g.rect(x0, y0 - 1, x1, y0 + 2, hair, hm)
            g.rect(x0, y0 + 2, x0 + 2, y0 + 4, hair, hm)
        }
        if (hs == 16) {
            g.rect(cx - 3, y0 - 2, cx + 3, y0 + 1, hair, hm)
            g.rect(cx - 2, y0 + 1, cx + 2, y0 + 3, hair, hm)
        }
        if (hs == 17) {
            g.rect(cx - 3, y0 + 1, cx + 1, y0 + 3, hair, hm)
            g.put(cx + 2, y0 + 2, hairD, hm)
        }
        if (hs == 18) {
            g.rect(cx - 3, y0 - 2, cx + 3, y0 + 1, hair, hm)
            g.put(cx + 3, y0 - 3, hair, hm)
        }
        if (hs == 19) {
            g.rect(cx - 3, y0, cx + 3, y0 + 2, hair, hm)
        }
        when (hs) {
            20 -> { // corte militar: topo baixo e uniforme
                g.rect(x0 + 1, y0 - 1, x1 - 1, y0 + 1, hair, hm)
                g.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 2, hairD, hm)
            }
            21 -> { // pompadour alto
                g.rect(cx - 3, y0 - 4, cx + 3, y0, hair, hm)
                g.rect(cx - 2, y0 - 5, cx + 2, y0 - 3, hairD, hm)
                g.rect(x0 + 1, y0, x1 - 1, y0 + 2, hair, hm)
            }
            22 -> { // penteado para trás
                g.rect(cx - 3, y0 - 3, cx + 3, y0, hair, hm)
                g.line(cx - 2, y0 - 2, x1 - 1, y0 + 1, hairD, hm)
                g.rect(x0 + 1, y0, x1 - 1, y0 + 2, hair, hm)
            }
            23 -> { // franja curta do mullet
                g.rect(x0 + 1, y0 - 1, x1 - 1, y0 + 2, hair, hm)
                g.put(cx - 2, y0 + 2, hairD, hm); g.put(cx + 2, y0 + 2, hairD, hm)
            }
            24 -> { // shag em camadas
                g.rect(x0, y0 - 2, x1, y0 + 2, hair, hm)
                g.put(x0 + 1, y0 + 3, hairD, hm); g.put(cx, y0 + 3, hair, hm); g.put(x1 - 2, y0 + 3, hairD, hm)
            }
            25 -> { // cortina repartida ao meio
                g.rect(x0, y0 - 1, cx - 1, y0 + 3, hair, hm)
                g.rect(cx + 1, y0 - 1, x1, y0 + 3, hair, hm)
                g.put(cx - 1, y0 + 3, hairD, hm); g.put(cx + 1, y0 + 3, hairD, hm)
            }
            26 -> { // franja bagunçada
                for (xx in x0 until x1 step 2) g.put(xx, y0 - (xx % 3), hair, hm)
                g.rect(x0 + 1, y0 + 1, x1 - 1, y0 + 3, hair, hm)
            }
            27 -> { // risco lateral
                g.rect(x0, y0 - 1, x1, y0 + 2, hair, hm)
                g.line(cx + 1, y0 - 1, cx + 1, y0 + 2, hairD, hm)
                g.rect(cx - 3, y0 + 1, cx + 3, y0 + 3, hair, hm)
            }
            28 -> { // cachos curtos
                for (xx in x0 until x1 step 2) {
                    g.ell(xx.toFloat(), (y0 - 1 + (xx % 2)).toFloat(), 1.5f, 1.8f, hair, hm)
                }
            }
            29 -> { // preso: faixa frontal discreta
                g.rect(x0, y0 - 1, x1, y0 + 2, hair, hm)
                g.rect(x0 + 1, y0 + 2, x1 - 1, y0 + 3, hairD, hm)
            }
        }
    }
    // detalhes do rosto
    when (L.accessory) {
        1 -> for (k in 0 until 5) g.put(x1 - 3 + k / 2, y0 + 3 + k, rgb(168, 80, 80), DETAIL)
        2 -> {
            val black = rgb(20, 20, 24)
            g.rect(ex1 - 1, ey - 1, ex1 + 2, ey + 2, black, DETAIL); g.line(x0, y0 + 2, x1 - 1, y0 + 3, black, DETAIL)
        }
        3 -> {
            val paint = rgb(180, 50, 50)
            g.rect(ex1 - 1, ey + 2, ex1 + 3, ey + 3, paint, DETAIL); g.rect(ex2 - 1, ey + 2, ex2 + 3, ey + 3, paint, DETAIL)
        }
        4 -> {
            val frame = rgb(60, 60, 80)
            for (ex in listOf(ex1, ex2)) {
                g.rect(ex - 1, ey - 1, ex + 3, ey, frame, DETAIL); g.rect(ex - 1, ey + 2, ex + 3, ey + 3, frame, DETAIL)
                g.put(ex - 1, ey, frame, DETAIL); g.put(ex - 1, ey + 1, frame, DETAIL)
                g.put(ex + 2, ey, frame, DETAIL); g.put(ex + 2, ey + 1, frame, DETAIL)
            }
            g.rect(cx - 1, ey, cx + 1, ey + 1, frame, DETAIL)
        }
        5 -> { g.put(x0 - 1, y0 + 7, GOLD, DETAIL); g.put(x1, y0 + 7, GOLD, DETAIL) }
        6 -> { val rune = rgb(90, 190, 210); g.put(cx, y0 + 2, rune, DETAIL); g.put(cx - 1, y0 + 3, rune, DETAIL); g.put(cx + 1, y0 + 3, rune, DETAIL); g.put(cx, y0 + 4, rune, DETAIL) }
        7 -> { val paint = rgb(150, 45, 45); g.line(x0 + 1, ey - 1, x1 - 2, ey + 2, paint, DETAIL) }
        8 -> { g.put(x0 - 1, y0 + 7, GOLD, DETAIL); g.put(x0 - 2, y0 + 8, GOLD, DETAIL) }
    }
    // chifres do tiefling
    if (L.race == 4) {
        val hc = rgb(70, 40, 50)
        for (sd in listOf(-1, 1)) {
            val bx = if (sd < 0) x0 + 1 else x1 - 2
            g.put(bx, y0 - 1, hc, DETAIL); g.put(bx + sd, y0 - 2, hc, DETAIL)
            g.put(bx + sd * 2, y0 - 3, hc, DETAIL); g.put(bx + sd * 2, y0 - 4, hc, DETAIL)
        }
    }
    // chapéus e elmos
    when (L.headgear) {
        1 -> {
            val hood = darkC(cloth, .2f)
            g.rect(x0 - 1, y0 - 2, x1 + 1, y0 + 3, hood)
            g.rect(x0 - 1, y0 + 3, x0 + 1, y0 + 9, hood); g.rect(x1 - 1, y0 + 3, x1 + 1, y0 + 9, hood)
            g.rect(x0, y0 + 3, x1, y0 + 4, darkC(cloth, .4f), DETAIL)
        }
        2 -> {
            g.rect(x0 - 1, y0 - 2, x1 + 1, y0 + 5, STEEL); g.rect(cx - 1, y0 + 5, cx + 1, y0 + 9, STEEL)
            g.rect(x0 - 1, y0 + 4, x1 + 1, y0 + 5, darkC(STEEL, .3f), DETAIL); g.rect(cx - 1, y0 - 4, cx + 1, y0 - 2, cloth)
        }
        3 -> {
            g.rect(x0 - 3, y0, x1 + 3, y0 + 2, cloth)
            for ((i, wd) in listOf(8, 8, 6, 6, 4, 4, 2).withIndex()) g.rect(cx - wd / 2, y0 - 1 - i, cx + wd / 2, y0 - i, cloth)
            g.rect(x0, y0 - 1, x1, y0, GOLD, DETAIL)
        }
        4 -> {
            g.rect(x0 + 1, y0 - 1, x1 - 1, y0 + 1, GOLD)
            for (xx in x0 + 1 until x1 - 1 step 2) g.put(xx, y0 - 2, GOLD, g.newId())
            g.put(cx, y0, rgb(200, 50, 60), DETAIL)
        }
        5 -> { g.rect(x0 + 1, y0 - 1, x1 - 1, y0, GOLD); g.put(cx, y0 - 2, GOLD, DETAIL); g.put(cx - 1, y0 - 2, GOLD, DETAIL) }
        6 -> { g.rect(x0 - 1, y0 - 2, x1 + 1, y0 + 7, STEEL); g.rect(x0, y0 + 6, x1, y0 + 8, darkC(STEEL, .35f), DETAIL); g.rect(cx - 1, y0 + 3, cx + 1, y0 + 5, 0, 0) }
        7 -> { g.rect(x0 - 1, y0 + 1, x1 + 1, y0 + 3, rgb(120, 35, 45)); g.put(cx, y0 + 2, GOLD, DETAIL) }
        8 -> { // máscara de couro cobrindo nariz e boca
            g.rect(x0 + 1, y0 + 7, x1 - 1, y0 + 10, darkC(LEATHER, .18f), DETAIL)
            g.put(cx, y0 + 8, GOLD, DETAIL)
        }
        9 -> { // elmo com chifres laterais
            g.rect(x0 - 1, y0 - 2, x1 + 1, y0 + 5, STEEL, DETAIL)
            g.rect(x0 - 3, y0 - 5, x0 - 1, y0 - 2, rgb(205, 195, 170), DETAIL)
            g.rect(x1 + 1, y0 - 5, x1 + 3, y0 - 2, rgb(205, 195, 170), DETAIL)
            g.rect(cx - 1, y0 + 4, cx + 1, y0 + 7, GOLD, DETAIL)
        }
        10 -> { // capuz aberto com borda emoldurando o rosto
            val hood = darkC(cloth, .22f)
            g.rect(x0 - 2, y0 - 2, x0 + 1, y0 + 8, hood, DETAIL)
            g.rect(x1 - 1, y0 - 2, x1 + 2, y0 + 8, hood, DETAIL)
            g.rect(x0 - 1, y0 - 2, x1 + 1, y0, hood, DETAIL)
            g.rect(x0, y0 + 7, x0 + 1, y0 + 9, hood, DETAIL)
            g.rect(x1 - 1, y0 + 7, x1, y0 + 9, hood, DETAIL)
        }
    }
    // armas na mão (esquerda da imagem) e escudo
    val w = L.weapon; val hx = tx0 - 2; val hy = ty + 8
    if (w == 1 || w == 5) {
        g.rect(hx, hy - 10, hx + 1, hy - 1, lightC(STEEL, .25f)); g.rect(hx - 1, hy - 1, hx + 2, hy, GOLD); g.rect(hx, hy, hx + 1, hy + 2, LEATHER)
    }
    if (w == 2) {
        g.rect(hx, hy - 14, hx + 1, hy + 10, LEATHER); g.ell(hx.toFloat(), (hy - 15).toFloat(), 2f, 2f, cloth); g.put(hx, hy - 15, lightC(cloth, .5f), DETAIL)
    }
    if (w == 3) {
        val m = g.newId()
        val pts = listOf(Pair(hx - 1, hy - 8), Pair(hx - 2, hy - 6), Pair(hx - 2, hy - 3), Pair(hx - 2, hy), Pair(hx - 2, hy + 3), Pair(hx - 2, hy + 6), Pair(hx - 1, hy + 8))
        for ((k, p) in pts.withIndex()) {
            g.put(p.first, p.second, LEATHER, m)
            if (k in 1..5) g.put(p.first, p.second + 1, LEATHER, m)
        }
        g.line(hx - 1, hy - 8, hx - 1, hy + 9, rgb(220, 220, 210), DETAIL)
    }
    if (w == 4) {
        g.rect(hx, hy - 10, hx + 1, hy + 8, LEATHER); g.rect(hx - 2, hy - 10, hx + 1, hy - 6, STEEL); g.rect(hx + 1, hy - 9, hx + 2, hy - 7, STEEL)
    }
    if (w == 5) {
        val sx = tx1 + 1; val sy = ty + 2
        g.rect(sx, sy, sx + 6, sy + 7, cloth); g.rect(sx + 1, sy + 7, sx + 5, sy + 8, cloth); g.rect(sx + 2, sy + 8, sx + 4, sy + 9, cloth)
        g.rect(sx, sy, sx + 6, sy + 1, STEEL, DETAIL); g.rect(sx + 2, sy + 2, sx + 4, sy + 5, GOLD, DETAIL)
    }
    if (w == 6) { g.rect(hx, hy - 5, hx + 1, hy + 2, LEATHER); g.rect(hx - 1, hy - 7, hx + 2, hy - 5, STEEL) }
    if (w == 7) { g.rect(hx, hy - 15, hx + 1, hy + 8, LEATHER); g.line(hx - 2, hy - 14, hx + 2, hy - 14, STEEL, DETAIL); g.put(hx, hy - 16, STEEL, DETAIL) }
    if (w == 8) { g.rect(hx, hy - 12, hx + 1, hy + 4, LEATHER); g.rect(hx - 2, hy - 15, hx + 2, hy - 11, STEEL); g.put(hx - 1, hy - 16, STEEL, DETAIL); g.put(hx + 1, hy - 16, STEEL, DETAIL) }

    // --- acabamento: luz no topo/esquerda, sombra embaixo/direita de cada peça, depois o contorno escuro
    fun opaque(x: Int, y: Int) = x in 0 until SPRITE_W && y in 0 until SPRITE_H && (g.c[y * SPRITE_W + x] ushr 24) != 0
    fun edge(x: Int, y: Int, nx: Int, ny: Int): Boolean {
        if (!opaque(nx, ny)) return true
        val other = g.matAt(nx, ny)
        return other != g.matAt(x, y) && other != DETAIL
    }
    val shaded = g.c.copyOf()
    for (y in 0 until SPRITE_H) for (x in 0 until SPRITE_W) {
        if (!opaque(x, y) || g.matAt(x, y) == DETAIL) continue
        var c = g.c[y * SPRITE_W + x]
        if (edge(x, y, x, y - 1)) c = lightC(c, .18f) else if (edge(x, y, x - 1, y)) c = lightC(c, .08f)
        if (edge(x, y, x, y + 1)) c = darkC(c, .22f) else if (edge(x, y, x + 1, y)) c = darkC(c, .12f)
        shaded[y * SPRITE_W + x] = c
    }
    val out = shaded.copyOf()
    for (y in 0 until SPRITE_H) for (x in 0 until SPRITE_W) {
        if (opaque(x, y)) continue
        if (opaque(x + 1, y) || opaque(x - 1, y) || opaque(x, y + 1) || opaque(x, y - 1)) out[y * SPRITE_W + x] = OUTLINE
    }
    return out
}

    .org $800
    .include "render.i"

expand_vec:
    .addr expand_0
    .addr expand_2
    .addr expand_4
    .addr expand_6
    .addr expand_8
    .addr expand_10
    .addr expand_12
    .addr expand_14
    .addr expand_16
    .addr expand_18
    .addr expand_20
    .addr expand_22
    .addr expand_24
    .addr expand_26
    .addr expand_28
    .addr expand_30
    .addr expand_32
    .addr expand_34
    .addr expand_36
    .addr expand_38
    .addr expand_40
    .addr expand_42
    .addr expand_44
    .addr expand_46
    .addr expand_48
    .addr expand_50
    .addr expand_52
    .addr expand_54
    .addr expand_56
    .addr expand_58
    .addr expand_60
    .addr expand_62
    .addr expand_64
    .addr expand_66
    .addr expand_68
    .addr expand_70
    .addr expand_72
    .addr expand_74
    .addr expand_76
    .addr expand_78
    .addr expand_80
    .addr expand_82
    .addr expand_84
    .addr expand_86
    .addr expand_88
    .addr expand_90
    .addr expand_92
    .addr expand_94
    .addr expand_96
    .addr expand_98
    .addr expand_100
    .addr expand_102
    .addr expand_104
    .addr expand_106
    .addr expand_108
    .addr expand_110
    .addr expand_112
    .addr expand_114
    .addr expand_116
    .addr expand_118
    .addr expand_120
    .addr expand_122
    .addr expand_124
    .addr expand_126
    .addr expand_128
    .addr expand_128
    .addr expand_132
    .addr expand_132
    .addr expand_136
    .addr expand_136
    .addr expand_140
    .addr expand_140
    .addr expand_144
    .addr expand_144
    .addr expand_148
    .addr expand_148
    .addr expand_152
    .addr expand_152
    .addr expand_156
    .addr expand_156
    .addr expand_160
    .addr expand_160
    .addr expand_164
    .addr expand_164
    .addr expand_168
    .addr expand_168
    .addr expand_172
    .addr expand_172
    .addr expand_176
    .addr expand_176
    .addr expand_180
    .addr expand_180
    .addr expand_184
    .addr expand_184
    .addr expand_188
    .addr expand_188
    .addr expand_192
    .addr expand_192
    .addr expand_192
    .addr expand_192
    .addr expand_200
    .addr expand_200
    .addr expand_200
    .addr expand_200
    .addr expand_208
    .addr expand_208
    .addr expand_208
    .addr expand_208
    .addr expand_216
    .addr expand_216
    .addr expand_216
    .addr expand_216
    .addr expand_224
    .addr expand_224
    .addr expand_224
    .addr expand_224
    .addr expand_232
    .addr expand_232
    .addr expand_232
    .addr expand_232
    .addr expand_240
    .addr expand_240
    .addr expand_240
    .addr expand_240
    .addr expand_248
    .addr expand_248
    .addr expand_248
    .addr expand_248

    .include "expand_hdr.i"
e_28rooto:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:   bvc e_30to

e_28toroto:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_29roto:
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:   asl
e_30to:
    bmi :+
    sta 30*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 31*BLIT_STRIDE + blitRoll,x
:   rts

e_t28oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   bvc e_29roto

e_t28rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:   bvc e_30to

e_48rooto:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50to

e_r48ooto:
    asl
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50to

e_48toroto:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_49roto:
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   asl
e_50to:
    bmi :+
    sta 50*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 51*BLIT_STRIDE + blitRoll,x
:   rts

e_r48toroto:
    asl
    bvc e_48toroto

e_t48oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   bvc e_49roto

e_t48rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50to

e_52rotoro:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_54ro

e_r52otoro:
    asl
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
    sta 53*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_54ro

e_52tooro:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:
e_t53oro:
    iny
    lda (pTex),y
e_53oro:
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:
e_54ro:
    bmi :+
    sta 54*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 55*BLIT_STRIDE + blitRoll,x
:   rts

e_r52tooro:
    asl
    bvc e_52tooro

e_t52rotoro:
    iny
    lda (pTex),y
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_54ro

e_76ooto:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78to

e_76rooto:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78to

e_r76ooto:
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78to

e_76toroto:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_77roto:
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   asl
e_78to:
    bmi :+
    sta 78*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 79*BLIT_STRIDE + blitRoll,x
:   rts

e_r76toroto:
    asl
    bvc e_76toroto

e_t76oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   bvc e_77roto

e_t76rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78to

e_84rooto:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86to

e_r84ooto:
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86to

e_84toroto:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_85roto:
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   asl
e_86to:
    bmi :+
    sta 86*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 87*BLIT_STRIDE + blitRoll,x
:   rts

e_r84toroto:
    asl
    bvc e_84toroto

e_t84oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   bvc e_85roto

e_t84rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86to

e_60tooro:
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 61*BLIT_STRIDE + blitRoll,x
:   bvc e_62ro

e_r60otoro:
    asl
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
:   bvc e_61toro

e_r60tooro:
    asl
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 61*BLIT_STRIDE + blitRoll,x
:   bvc e_62ro

e_60rotoro:
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
:   asl
e_61toro:
    bmi :+
    sta 61*BLIT_STRIDE + blitRoll,x
:
e_t62ro:
    iny
    lda (pTex),y
e_62ro:
    bmi :+
    sta 62*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 63*BLIT_STRIDE + blitRoll,x
:   rts

e_t60rotoro:
    iny
    lda (pTex),y
    bvc e_60rotoro

e_r36otoro:
    asl
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
    sta 37*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_38ro

e_r36tooro:
    asl
    bvc e_36tooro

e_36tooro:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:
e_t37oro:
    iny
    lda (pTex),y
e_37oro:
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:
e_38ro:
    bmi :+
    sta 38*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 39*BLIT_STRIDE + blitRoll,x
:   rts

e_t36ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   bvc e_37oro

e_t36rotoro:
    iny
    lda (pTex),y
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_38ro

e_52rooto:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   bvc e_54to

e_52toroto:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_53roto:
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   asl
e_54to:
    bmi :+
    sta 54*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 55*BLIT_STRIDE + blitRoll,x
:   rts

e_r52toroto:
    asl
    bvc e_52toroto

e_t52oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   bvc e_53roto

e_56rooto:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   bvc e_58to

e_r56ooto:
    asl
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
    sta 57*BLIT_STRIDE + blitRoll,x
:   bvc e_58to

e_56toroto:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_57roto:
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   asl
e_58to:
    bmi :+
    sta 58*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 59*BLIT_STRIDE + blitRoll,x
:   rts

e_r56toroto:
    asl
    bvc e_56toroto

e_t56oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   bvc e_57roto

e_t56rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   bvc e_58to

e_72rooto:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74to

e_r72ooto:
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74to

e_72toroto:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_73roto:
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   asl
e_74to:
    bmi :+
    sta 74*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 75*BLIT_STRIDE + blitRoll,x
:   rts

e_r72toroto:
    asl
    bvc e_72toroto

e_t72oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   bvc e_73roto

e_t72rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74to

e_80rooto:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   bvc e_82to

e_r80ooto:
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
:   bvc e_82to

e_80toroto:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_81roto:
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   asl
e_82to:
    bmi :+
    sta 82*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 83*BLIT_STRIDE + blitRoll,x
:   rts

e_r80toroto:
    asl
    bvc e_80toroto

e_t80oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81roto

e_88rooto:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   bvc e_90to

e_88toroto:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_89roto:
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   asl
e_90to:
    bmi :+
    sta 90*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 91*BLIT_STRIDE + blitRoll,x
:   rts

e_r88toroto:
    asl
    bvc e_88toroto

e_t88oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   bvc e_89roto

e_t88rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   bvc e_90to

e_92rooto:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94to

e_r92ooto:
    asl
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94to

e_92toroto:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_93roto:
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   asl
e_94to:
    bmi :+
    sta 94*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 95*BLIT_STRIDE + blitRoll,x
:   rts

e_r92toroto:
    asl
    bvc e_92toroto

e_t92oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   bvc e_93roto

e_t92rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94to

e_96rooto:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98to

e_r96ooto:
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98to

e_96toroto:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_97roto:
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   asl
e_98to:
    bmi :+
    sta 98*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 99*BLIT_STRIDE + blitRoll,x
:   rts

e_r96toroto:
    asl
    bvc e_96toroto

e_t96oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   bvc e_97roto

e_t96rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98to

e_32tooro:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   bvc e_34ro

e_r32otoro:
    asl
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33toro

e_r32tooro:
    asl
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   bvc e_34ro

e_32rotoro:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   asl
e_33toro:
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_34ro:
    bmi :+
    sta 34*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 35*BLIT_STRIDE + blitRoll,x
:   rts

e_t32ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
    sta 33*BLIT_STRIDE + blitRoll,x
:   bvc e_34ro

e_t32rotoro:
    iny
    lda (pTex),y
    bvc e_32rotoro

e_40tooro:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   bvc e_42ro

e_r40otoro:
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   bvc e_41toro

e_r40tooro:
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   bvc e_42ro

e_40rotoro:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   asl
e_41toro:
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_42ro:
    bmi :+
    sta 42*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 43*BLIT_STRIDE + blitRoll,x
:   rts

e_t40ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
:   bvc e_42ro

e_t40rotoro:
    iny
    lda (pTex),y
    bvc e_40rotoro

e_44tooro:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   bvc e_46ro

e_r44otoro:
    asl
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   bvc e_45toro

e_r44tooro:
    asl
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   bvc e_46ro

e_44rotoro:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   asl
e_45toro:
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_46ro:
    bmi :+
    sta 46*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 47*BLIT_STRIDE + blitRoll,x
:   rts

e_t44ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
    sta 45*BLIT_STRIDE + blitRoll,x
:   bvc e_46ro

e_t44rotoro:
    iny
    lda (pTex),y
    bvc e_44rotoro

e_48tooro:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50ro

e_r48tooro:
    asl
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50ro

e_48rotoro:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   asl
e_49toro:
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_50ro:
    bmi :+
    sta 50*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 51*BLIT_STRIDE + blitRoll,x
:   rts

e_t48ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
:   bvc e_50ro

e_t48rotoro:
    iny
    lda (pTex),y
    bvc e_48rotoro

e_72tooro:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74ro

e_r72otoro:
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   bvc e_73toro

e_72rotoro:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
e_73toro:
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_74ro:
    bmi :+
    sta 74*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 75*BLIT_STRIDE + blitRoll,x
:   rts

e_r72tooro:
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74ro

e_t72ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
    sta 73*BLIT_STRIDE + blitRoll,x
:   bvc e_74ro

e_t72rotoro:
    iny
    lda (pTex),y
    bvc e_72rotoro

e_80ooro:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
:   bvc e_82ro

e_80tooro:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   bvc e_82ro

e_r80otoro:
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81toro

e_80rotoro:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
e_81toro:
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_82ro:
    bmi :+
    sta 82*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 83*BLIT_STRIDE + blitRoll,x
:   rts

e_r80tooro:
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   bvc e_82ro

e_t80rotoro:
    iny
    lda (pTex),y
    bvc e_80rotoro

e_96tooro:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98ro

e_96rotoro:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
e_97toro:
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_98ro:
    bmi :+
    sta 98*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 99*BLIT_STRIDE + blitRoll,x
:   rts

e_r96tooro:
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98ro

e_t96ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
    sta 97*BLIT_STRIDE + blitRoll,x
:   bvc e_98ro

e_r40ooto:
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   bvc e_41oto

e_40rooto:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   asl
e_41oto:
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:
e_42to:
    bmi :+
    sta 42*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 43*BLIT_STRIDE + blitRoll,x
:   rts

e_r40toroto:
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_42to

e_t40oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_42to

e_t40rooto:
    iny
    lda (pTex),y
    bvc e_40rooto

e_28rotoro:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_30ro

e_28tooro:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:
e_t29oro:
    iny
    lda (pTex),y
e_29oro:
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:
e_30ro:
    bmi :+
    sta 30*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 31*BLIT_STRIDE + blitRoll,x
:   rts

e_r28tooro:
    asl
    bvc e_28tooro

e_56tooro:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   bvc e_58ro

e_r56otoro:
    asl
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   bvc e_57toro

e_56rotoro:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   asl
e_57toro:
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_58ro:
    bmi :+
    sta 58*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 59*BLIT_STRIDE + blitRoll,x
:   rts

e_r56tooro:
    asl
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   bvc e_58ro

e_t56rotoro:
    iny
    lda (pTex),y
    bvc e_56rotoro

e_76tooro:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78ro

e_r76otoro:
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   bvc e_77toro

e_76rotoro:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
e_77toro:
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_78ro:
    bmi :+
    sta 78*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 79*BLIT_STRIDE + blitRoll,x
:   rts

e_r76tooro:
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78ro

e_t76ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
:   bvc e_78ro

e_t76rotoro:
    iny
    lda (pTex),y
    bvc e_76rotoro

e_84tooro:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86ro

e_84rotoro:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
e_85toro:
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_86ro:
    bmi :+
    sta 86*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 87*BLIT_STRIDE + blitRoll,x
:   rts

e_r84tooro:
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86ro

e_t84ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
    sta 85*BLIT_STRIDE + blitRoll,x
:   bvc e_86ro

e_t84rotoro:
    iny
    lda (pTex),y
    bvc e_84rotoro

e_92tooro:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94ro

e_r92otoro:
    asl
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   bvc e_93toro

e_92rotoro:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
e_93toro:
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_94ro:
    bmi :+
    sta 94*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 95*BLIT_STRIDE + blitRoll,x
:   rts

e_r92tooro:
    asl
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94ro

e_t92ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
    sta 93*BLIT_STRIDE + blitRoll,x
:   bvc e_94ro

e_t92rotoro:
    iny
    lda (pTex),y
    bvc e_92rotoro

e_88tooro:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_89oro:
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:
e_90ro:
    bmi :+
    sta 90*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 91*BLIT_STRIDE + blitRoll,x
:   rts

e_r88tooro:
    asl
    bvc e_88tooro

e_t88ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   bvc e_89oro

e_t88rotoro:
    iny
    lda (pTex),y
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_90ro

e_r100ooto:
    asl
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   bvc e_101oto

e_100rooto:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   asl
e_101oto:
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:
e_102to:
    bmi :+
    sta 102*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 103*BLIT_STRIDE + blitRoll,x
:   rts

e_t100oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_102to

e_t100rooto:
    iny
    lda (pTex),y
    bvc e_100rooto

e_r104ooto:
    asl
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   bvc e_105oto

e_104rooto:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   asl
e_105oto:
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:
e_106to:
    bmi :+
    sta 106*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 107*BLIT_STRIDE + blitRoll,x
:   rts

e_t104rooto:
    iny
    lda (pTex),y
    bvc e_104rooto

e_r108ooto:
    asl
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   bvc e_109oto

e_108rooto:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   asl
e_109oto:
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
:
e_110to:
    bmi :+
    sta 110*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 111*BLIT_STRIDE + blitRoll,x
:   rts

e_r20ooto:
    asl
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   bvc e_21oto

e_20rooto:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   asl
e_21oto:
    bmi :+
    sta 21*BLIT_STRIDE + blitRoll,x
:
e_22to:
    bmi :+
    sta 22*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 23*BLIT_STRIDE + blitRoll,x
:   rts

e_t20oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
    sta 21*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_22to

e_r24ooto:
    asl
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   bvc e_25oto

e_24rooto:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   asl
e_25oto:
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
:
e_26to:
    bmi :+
    sta 26*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 27*BLIT_STRIDE + blitRoll,x
:   rts

e_t24oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
    sta 25*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_26to

e_28otoo:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   bvc e_29too

e_r28otoo:
    asl
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   bvc e_29too

e_28rotoo:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   asl
e_29too:
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:
e_t30o:
    iny
    lda (pTex),y
    bmi :+
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
:   rts

e_t28rotoo:
    iny
    lda (pTex),y
    bvc e_28rotoo

e_32otoo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33too

e_r32otoo:
    asl
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33too

e_32rotoo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   asl
e_33too:
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:
e_t34o:
    iny
    lda (pTex),y
    bmi :+
    sta 34*BLIT_STRIDE + blitRoll,x
    sta 35*BLIT_STRIDE + blitRoll,x
:   rts

e_t32rotoo:
    iny
    lda (pTex),y
    bvc e_32rotoo

e_36otoo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   bvc e_37too

e_r36otoo:
    asl
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   bvc e_37too

e_36rotoo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   asl
e_37too:
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:
e_t38o:
    iny
    lda (pTex),y
    bmi :+
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
:   rts

e_t36rotoo:
    iny
    lda (pTex),y
    bvc e_36rotoo

e_40otoo:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   bvc e_41too

e_r40otoo:
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   bvc e_41too

e_40rotoo:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   asl
e_41too:
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:
e_t42o:
    iny
    lda (pTex),y
    bmi :+
    sta 42*BLIT_STRIDE + blitRoll,x
    sta 43*BLIT_STRIDE + blitRoll,x
:   rts

e_44otoo:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   bvc e_45too

e_r44otoo:
    asl
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   bvc e_45too

e_44rotoo:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   asl
e_45too:
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:
e_t46o:
    iny
    lda (pTex),y
    bmi :+
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
:   rts

e_48otoo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   bvc e_49too

e_r48otoo:
    asl
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   bvc e_49too

e_48rotoo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   asl
e_49too:
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:
e_t50o:
    iny
    lda (pTex),y
    bmi :+
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
:   rts

e_52otoo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   bvc e_53too

e_r52otoo:
    asl
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   bvc e_53too

e_52rotoo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
e_53too:
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:
e_t54o:
    iny
    lda (pTex),y
    bmi :+
    sta 54*BLIT_STRIDE + blitRoll,x
    sta 55*BLIT_STRIDE + blitRoll,x
:   rts

e_t52rotoo:
    iny
    lda (pTex),y
    bvc e_52rotoo

e_r16tooro:
    asl
    bvc e_16tooro

e_16tooro:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:
e_t17oro:
    iny
    lda (pTex),y
e_17oro:
    bmi :+
    sta 17*BLIT_STRIDE + blitRoll,x
:
e_18ro:
    bmi :+
    sta 18*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 19*BLIT_STRIDE + blitRoll,x
:   rts

e_t16ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   bvc e_17oro

e_20tooro:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:
e_t21oro:
    iny
    lda (pTex),y
e_21oro:
    bmi :+
    sta 21*BLIT_STRIDE + blitRoll,x
:
e_22ro:
    bmi :+
    sta 22*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 23*BLIT_STRIDE + blitRoll,x
:   rts

e_r20tooro:
    asl
    bvc e_20tooro

e_24rotoro:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_26ro

e_r24tooro:
    asl
    bvc e_24tooro

e_24tooro:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:
e_t25oro:
    iny
    lda (pTex),y
e_25oro:
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
:
e_26ro:
    bmi :+
    sta 26*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 27*BLIT_STRIDE + blitRoll,x
:   rts

e_t24ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   bvc e_25oro

e_68tooro:
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   bvc e_70ro

e_r68otoro:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   bvc e_69toro

e_68rotoro:
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   asl
e_69toro:
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_70ro:
    bmi :+
    sta 70*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 71*BLIT_STRIDE + blitRoll,x
:   rts

e_r68tooro:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   bvc e_70ro

e_t68rotoro:
    iny
    lda (pTex),y
    bvc e_68rotoro

e_r36toroto:
    asl
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_38to

e_36rooto:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   asl
e_37oto:
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:
e_38to:
    bmi :+
    sta 38*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 39*BLIT_STRIDE + blitRoll,x
:   rts

e_t36oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
    sta 37*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_38to

e_t36rooto:
    iny
    lda (pTex),y
    bvc e_36rooto

e_r44ooto:
    asl
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   bvc e_45oto

e_r44toroto:
    asl
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_46to

e_44rooto:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   asl
e_45oto:
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:
e_46to:
    bmi :+
    sta 46*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 47*BLIT_STRIDE + blitRoll,x
:   rts

e_t44oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
    sta 45*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_46to

e_t44rooto:
    iny
    lda (pTex),y
    bvc e_44rooto

e_104ooro:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   bvc e_105oro

e_r104otoro:
    asl
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
    sta 105*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_106ro

e_104tooro:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_105oro:
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:
e_106ro:
    bmi :+
    sta 106*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 107*BLIT_STRIDE + blitRoll,x
:   rts

e_t104ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   bvc e_105oro

e_100rotoro:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_102ro

e_r100otoro:
    asl
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_102ro

e_100tooro:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_101oro:
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:
e_102ro:
    bmi :+
    sta 102*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 103*BLIT_STRIDE + blitRoll,x
:   rts

e_r100tooro:
    asl
    bvc e_100tooro

e_t100ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   bvc e_101oro

e_112rooto:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   asl
e_113oto:
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 115*BLIT_STRIDE + blitRoll,x
:   rts

e_r112ooto:
    asl
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   bvc e_113oto

e_t112rooto:
    iny
    lda (pTex),y
    bvc e_112rooto

e_72otoo:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   bvc e_73too

e_72rotoo:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
e_73too:
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 74*BLIT_STRIDE + blitRoll,x
    sta 75*BLIT_STRIDE + blitRoll,x
:   rts

e_r72otoo:
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   bvc e_73too

e_t72rotoo:
    iny
    lda (pTex),y
    bvc e_72rotoo

e_76otoo:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   bvc e_77too

e_76rotoo:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
e_77too:
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
:   rts

e_r76otoo:
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   bvc e_77too

e_80otoo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81too

e_80rotoo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
e_81too:
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 82*BLIT_STRIDE + blitRoll,x
    sta 83*BLIT_STRIDE + blitRoll,x
:   rts

e_r80otoo:
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81too

e_t80rotoo:
    iny
    lda (pTex),y
    bvc e_80rotoo

e_84otoo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   bvc e_85too

e_84rotoo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
e_85too:
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
:   rts

e_r84otoo:
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   bvc e_85too

e_t84rotoo:
    iny
    lda (pTex),y
    bvc e_84rotoo

e_92otoo:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   bvc e_93too

e_92rotoo:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
e_93too:
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 94*BLIT_STRIDE + blitRoll,x
    sta 95*BLIT_STRIDE + blitRoll,x
:   rts

e_r92otoo:
    asl
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   bvc e_93too

e_r16ooto:
    asl
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   bvc e_17oto

e_16rooto:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   asl
e_17oto:
    bmi :+
    sta 17*BLIT_STRIDE + blitRoll,x
:
e_18to:
    bmi :+
    sta 18*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 19*BLIT_STRIDE + blitRoll,x
:   rts

e_t16oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_18to

e_112tooro:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_113oro:
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 115*BLIT_STRIDE + blitRoll,x
:   rts

e_r112tooro:
    asl
    bvc e_112tooro

e_24oroo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   bvc e_25roo

e_24toroo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_25roo:
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
:   rts

e_t24oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   bvc e_25roo

e_32oroo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33roo

e_32toroo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_33roo:
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 34*BLIT_STRIDE + blitRoll,x
    sta 35*BLIT_STRIDE + blitRoll,x
:   rts

e_t32oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33roo

e_36oroo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   bvc e_37roo

e_36toroo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_37roo:
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
:   rts

e_t36oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   bvc e_37roo

e_48oroo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   bvc e_49roo

e_48toroo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_49roo:
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
:   rts

e_t48oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   bvc e_49roo

e_52oroo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   bvc e_53roo

e_52toroo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_53roo:
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 54*BLIT_STRIDE + blitRoll,x
    sta 55*BLIT_STRIDE + blitRoll,x
:   rts

e_r52toroo:
    asl
    bvc e_52toroo

e_t52oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   bvc e_53roo

e_80oroo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81roo

e_80toroo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_81roo:
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 82*BLIT_STRIDE + blitRoll,x
    sta 83*BLIT_STRIDE + blitRoll,x
:   rts

e_r80toroo:
    asl
    bvc e_80toroo

e_t80oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   bvc e_81roo

e_84oroo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   bvc e_85roo

e_84toroo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_85roo:
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
:   rts

e_r84toroo:
    asl
    bvc e_84toroo

e_t84oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   bvc e_85roo

e_12rooto:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_13oto

e_12ooto:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:
e_13oto:
    bmi :+
    sta 13*BLIT_STRIDE + blitRoll,x
:
e_14to:
    bmi :+
    sta 14*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 15*BLIT_STRIDE + blitRoll,x
:   rts

e_20rotoo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_21too

e_20otoo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:
e_21too:
    bmi :+
    sta 21*BLIT_STRIDE + blitRoll,x
:
e_t22o:
    iny
    lda (pTex),y
    bmi :+
    sta 22*BLIT_STRIDE + blitRoll,x
    sta 23*BLIT_STRIDE + blitRoll,x
:   rts

e_r20otoo:
    asl
    bvc e_20otoo

e_24rotoo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_25too

e_24otoo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:
e_25too:
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
:
e_t26o:
    iny
    lda (pTex),y
    bmi :+
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
:   rts

e_r24otoo:
    asl
    bvc e_24otoo

e_56rotoo:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_57too

e_56otoo:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:
e_57too:
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:
e_t58o:
    iny
    lda (pTex),y
    bmi :+
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
:   rts

e_r56otoo:
    asl
    bvc e_56otoo

e_32rooto:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   bvc e_34to

e_r32toroto:
    asl
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_33roto:
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   asl
e_34to:
    bmi :+
    sta 34*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 35*BLIT_STRIDE + blitRoll,x
:   rts

e_r32ooto:
    asl
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
    sta 33*BLIT_STRIDE + blitRoll,x
:   bvc e_34to

e_t32oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   bvc e_33roto

e_r68ooto:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
    sta 69*BLIT_STRIDE + blitRoll,x
:   bvc e_70to

e_r68toroto:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_69roto:
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   asl
e_70to:
    bmi :+
    sta 70*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 71*BLIT_STRIDE + blitRoll,x
:   rts

e_t68oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   bvc e_69roto

e_t68rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   bvc e_70to

e_r108tooro:
    asl
    bvc e_108tooro

e_108tooro:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_109oro:
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
    sta 110*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 111*BLIT_STRIDE + blitRoll,x
:   rts

e_t108ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   bvc e_109oro

e_100otoo:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:
e_101too:
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 102*BLIT_STRIDE + blitRoll,x
    sta 103*BLIT_STRIDE + blitRoll,x
:   rts

e_r100otoo:
    asl
    bvc e_100otoo

e_104rotoo:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_105too

e_104otoo:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:
e_105too:
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
:   rts

e_r104otoo:
    asl
    bvc e_104otoo

e_108otoo:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:
e_109too:
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
:   rts

e_r108otoo:
    asl
    bvc e_108otoo

e_112rotoo:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_113too

e_112otoo:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:
e_113too:
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 114*BLIT_STRIDE + blitRoll,x
    sta 115*BLIT_STRIDE + blitRoll,x
:   rts

e_r112otoo:
    asl
    bvc e_112otoo

e_116rooto:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_117oto

e_116ooto:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:
e_117oto:
    bmi :+
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 119*BLIT_STRIDE + blitRoll,x
:   rts

e_r116ooto:
    asl
    bvc e_116ooto

e_8rooto:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_9oto

e_8ooto:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:
e_9oto:
    bmi :+
    sta 9*BLIT_STRIDE + blitRoll,x
    sta 10*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 11*BLIT_STRIDE + blitRoll,x
:   rts

e_r8ooto:
    asl
    bvc e_8ooto

e_88rotoo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_89too

e_88otoo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:
e_89too:
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
:   rts

e_r88otoo:
    asl
    bvc e_88otoo

e_96rotoo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_97too

e_96otoo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:
e_97too:
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
:   rts

e_r96otoo:
    asl
    bvc e_96otoo

e_t12ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:   bvc e_13oro

e_12tooro:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:
e_t13oro:
    iny
    lda (pTex),y
e_13oro:
    bmi :+
    sta 13*BLIT_STRIDE + blitRoll,x
    sta 14*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 15*BLIT_STRIDE + blitRoll,x
:   rts

e_t4ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   bvc e_5oro

e_4tooro:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:
e_t5oro:
    iny
    lda (pTex),y
e_5oro:
    bmi :+
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 7*BLIT_STRIDE + blitRoll,x
:   rts

e_t8ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:   bvc e_9oro

e_8tooro:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:
e_t9oro:
    iny
    lda (pTex),y
e_9oro:
    bmi :+
    sta 9*BLIT_STRIDE + blitRoll,x
    sta 10*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 11*BLIT_STRIDE + blitRoll,x
:   rts

e_t0ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   bvc e_1oro

e_t1oro:
    iny
    lda (pTex),y
e_1oro:
    bmi :+
    sta 1*BLIT_STRIDE + blitRoll,x
    sta 2*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 3*BLIT_STRIDE + blitRoll,x
:   rts

e_r12otoo:
    asl
    bvc e_12otoo

e_12otoo:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
:
e_t14o:
    iny
    lda (pTex),y
    bmi :+
    sta 14*BLIT_STRIDE + blitRoll,x
    sta 15*BLIT_STRIDE + blitRoll,x
:   rts

e_r16otoo:
    asl
    bvc e_16otoo

e_16otoo:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
:
e_t18o:
    iny
    lda (pTex),y
    bmi :+
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
:   rts

e_r4otoo:
    asl
    bvc e_4otoo

e_4otoo:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
    sta 5*BLIT_STRIDE + blitRoll,x
:
e_t6o:
    iny
    lda (pTex),y
    bmi :+
    sta 6*BLIT_STRIDE + blitRoll,x
    sta 7*BLIT_STRIDE + blitRoll,x
:   rts

e_r8otoo:
    asl
    bvc e_8otoo

e_8otoo:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
    sta 9*BLIT_STRIDE + blitRoll,x
:
e_t10o:
    iny
    lda (pTex),y
    bmi :+
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
:   rts

e_r108ooo:
    asl
e_108ooo:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:
e_109oo:
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
:   rts

e_108tooo:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_109oo

e_12rooo:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_13oo

e_r12ooo:
    asl
e_12ooo:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:
e_13oo:
    bmi :+
    sta 13*BLIT_STRIDE + blitRoll,x
    sta 14*BLIT_STRIDE + blitRoll,x
    sta 15*BLIT_STRIDE + blitRoll,x
:   rts

e_12tooo:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_13oo

e_t12ooo:
    iny
    lda (pTex),y
    bvc e_12ooo

e_124rooo:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_125oo

e_r124ooo:
    asl
e_124ooo:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:
e_125oo:
    bmi :+
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
:   rts

e_124tooo:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_125oo

e_88rooo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_89oo

e_r88ooo:
    asl
e_88ooo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:
e_89oo:
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
:   rts

e_88tooo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_89oo

e_100oroo:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:
e_101roo:
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 102*BLIT_STRIDE + blitRoll,x
    sta 103*BLIT_STRIDE + blitRoll,x
:   rts

e_t100oroo:
    iny
    lda (pTex),y
    bvc e_100oroo

e_104oroo:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:
e_105roo:
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
:   rts

e_t104oroo:
    iny
    lda (pTex),y
    bvc e_104oroo

e_112oroo:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:
e_113roo:
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 114*BLIT_STRIDE + blitRoll,x
    sta 115*BLIT_STRIDE + blitRoll,x
:   rts

e_t112oroo:
    iny
    lda (pTex),y
    bvc e_112oroo

e_116tooro:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_117oro

e_116ooro:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:
e_117oro:
    bmi :+
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 119*BLIT_STRIDE + blitRoll,x
:   rts

e_16toroo:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_17roo

e_16oroo:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:
e_17roo:
    bmi :+
    sta 17*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
:   rts

e_t16oroo:
    iny
    lda (pTex),y
    bvc e_16oroo

e_20toroo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_21roo

e_20oroo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:
e_21roo:
    bmi :+
    sta 21*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 22*BLIT_STRIDE + blitRoll,x
    sta 23*BLIT_STRIDE + blitRoll,x
:   rts

e_t20oroo:
    iny
    lda (pTex),y
    bvc e_20oroo

e_28oroo:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:
e_29roo:
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
:   rts

e_t28oroo:
    iny
    lda (pTex),y
    bvc e_28oroo

e_40oroo:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:
e_41roo:
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 42*BLIT_STRIDE + blitRoll,x
    sta 43*BLIT_STRIDE + blitRoll,x
:   rts

e_t40oroo:
    iny
    lda (pTex),y
    bvc e_40oroo

e_44toroo:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_45roo

e_44oroo:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:
e_45roo:
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
:   rts

e_t44oroo:
    iny
    lda (pTex),y
    bvc e_44oroo

e_56oroo:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:
e_57roo:
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
:   rts

e_t56oroo:
    iny
    lda (pTex),y
    bvc e_56oroo

e_72oroo:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:
e_73roo:
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 74*BLIT_STRIDE + blitRoll,x
    sta 75*BLIT_STRIDE + blitRoll,x
:   rts

e_t72oroo:
    iny
    lda (pTex),y
    bvc e_72oroo

e_76oroo:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:
e_77roo:
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
:   rts

e_t76oroo:
    iny
    lda (pTex),y
    bvc e_76oroo

e_88oroo:
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:
e_89roo:
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
:   rts

e_t88oroo:
    iny
    lda (pTex),y
    bvc e_88oroo

e_92oroo:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:
e_93roo:
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 94*BLIT_STRIDE + blitRoll,x
    sta 95*BLIT_STRIDE + blitRoll,x
:   rts

e_t92oroo:
    iny
    lda (pTex),y
    bvc e_92oroo

e_96toroo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_97roo

e_96oroo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:
e_97roo:
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
:   rts

e_t96oroo:
    iny
    lda (pTex),y
    bvc e_96oroo

e_t64ooro:
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   bvc e_65oro

e_64tooro:
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_65oro:
    bmi :+
    sta 65*BLIT_STRIDE + blitRoll,x
:
e_66ro:
    bmi :+
    sta 66*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 67*BLIT_STRIDE + blitRoll,x
:   rts

e_t64rotoro:
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 65*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_66ro

e_120otoo:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
:   rts

e_r120otoo:
    asl
    bvc e_120otoo

e_124otoo:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
    sta 125*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
:   rts

e_r124otoo:
    asl
    bvc e_124otoo

e_104tooo:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_105oo

e_104rooo:
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   asl
e_105oo:
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
:   rts

e_116tooo:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_117oo

e_116rooo:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   asl
e_117oo:
    bmi :+
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
:   rts

e_28tooo:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_29oo

e_28rooo:
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   asl
e_29oo:
    bmi :+
    sta 29*BLIT_STRIDE + blitRoll,x
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
:   rts

e_36tooo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_37oo

e_36rooo:
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   asl
e_37oo:
    bmi :+
    sta 37*BLIT_STRIDE + blitRoll,x
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
:   rts

e_4tooo:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_5oo

e_4rooo:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   asl
e_5oo:
    bmi :+
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
    sta 7*BLIT_STRIDE + blitRoll,x
:   rts

e_40tooo:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_41oo

e_40rooo:
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   asl
e_41oo:
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
    sta 42*BLIT_STRIDE + blitRoll,x
    sta 43*BLIT_STRIDE + blitRoll,x
:   rts

e_44rooo:
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   asl
e_45oo:
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
:   rts

e_76tooo:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_77oo

e_76rooo:
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
e_77oo:
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
:   rts

e_8tooo:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_9oo

e_8rooo:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:   asl
e_9oo:
    bmi :+
    sta 9*BLIT_STRIDE + blitRoll,x
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
:   rts

e_80tooo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_81oo

e_80rooo:
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
e_81oo:
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
    sta 82*BLIT_STRIDE + blitRoll,x
    sta 83*BLIT_STRIDE + blitRoll,x
:   rts

e_84tooo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_85oo

e_84rooo:
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
e_85oo:
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
:   rts

e_96tooo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_97oo

e_96rooo:
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
e_97oo:
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
:   rts

e_t0rooo:
    iny
    lda (pTex),y
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   asl
e_1oo:
    bmi :+
    sta 1*BLIT_STRIDE + blitRoll,x
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
:   rts

e_tr0tooo:
    iny
    lda (pTex),y
    asl
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_1oo

e_108oroo:
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
    sta 109*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
:   rts

e_t108oroo:
    iny
    lda (pTex),y
    bvc e_108oroo

e_116oroo:
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
:   rts

e_t116oroo:
    iny
    lda (pTex),y
    bvc e_116oroo

e_12oroo:
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 14*BLIT_STRIDE + blitRoll,x
    sta 15*BLIT_STRIDE + blitRoll,x
:   rts

e_t12oroo:
    iny
    lda (pTex),y
    bvc e_12oroo

e_120oroo:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
:   rts

e_t120oroo:
    iny
    lda (pTex),y
    bvc e_120oroo

e_124oroo:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
    sta 125*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
:   rts

e_t124oroo:
    iny
    lda (pTex),y
    bvc e_124oroo

e_4oroo:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
    sta 5*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 6*BLIT_STRIDE + blitRoll,x
    sta 7*BLIT_STRIDE + blitRoll,x
:   rts

e_t4oroo:
    iny
    lda (pTex),y
    bvc e_4oroo

e_60oroo:
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
    sta 61*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 62*BLIT_STRIDE + blitRoll,x
    sta 63*BLIT_STRIDE + blitRoll,x
:   rts

e_t60oroo:
    iny
    lda (pTex),y
    bvc e_60oroo

e_8oroo:
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
    sta 9*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
:   rts

e_t8oroo:
    iny
    lda (pTex),y
    bvc e_8oroo

e_120rooto:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   asl
e_121oto:
    bmi :+
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 123*BLIT_STRIDE + blitRoll,x
:   rts

e_r120ooto:
    asl
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   bvc e_121oto

e_4rooto:
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   asl
e_5oto:
    bmi :+
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 7*BLIT_STRIDE + blitRoll,x
:   rts

e_r4ooto:
    asl
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   bvc e_5oto

e_68otoo:
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:
e_69too:
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
:   rts

e_68rotoo:
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   asl
    bvc e_69too

e_t0rooto:
    iny
    lda (pTex),y
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   asl
e_1oto:
    bmi :+
    sta 1*BLIT_STRIDE + blitRoll,x
    sta 2*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 3*BLIT_STRIDE + blitRoll,x
:   rts

e_tr0ooto:
    iny
    lda (pTex),y
    asl
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   bvc e_1oto

e_120ooro:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:
e_121oro:
    bmi :+
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 123*BLIT_STRIDE + blitRoll,x
:   rts

e_120tooro:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_121oro

e_124tooro:
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_125oro:
    bmi :+
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 127*BLIT_STRIDE + blitRoll,x
:   rts

e_r68toroo:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_69roo:
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
:   rts

e_t68oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   bvc e_69roo

e_t2o:
    iny
    lda (pTex),y
    bmi :+
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
:   rts

e_tr0otoo:
    iny
    lda (pTex),y
    asl
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
:   bvc e_t2o

e_100rooo:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   asl
e_101oo:
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
    sta 102*BLIT_STRIDE + blitRoll,x
    sta 103*BLIT_STRIDE + blitRoll,x
:   rts

e_100tooo:
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_101oo

e_112rooo:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   asl
e_113oo:
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
    sta 115*BLIT_STRIDE + blitRoll,x
:   rts

e_112tooo:
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_113oo

e_120rooo:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   asl
e_121oo:
    bmi :+
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
:   rts

e_120tooo:
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_121oo

e_16rooo:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   asl
e_17oo:
    bmi :+
    sta 17*BLIT_STRIDE + blitRoll,x
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
:   rts

e_16tooo:
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_17oo

e_20rooo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   asl
e_21oo:
    bmi :+
    sta 21*BLIT_STRIDE + blitRoll,x
    sta 22*BLIT_STRIDE + blitRoll,x
    sta 23*BLIT_STRIDE + blitRoll,x
:   rts

e_20tooo:
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_21oo

e_24rooo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   asl
e_25oo:
    bmi :+
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
:   rts

e_24tooo:
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_25oo

e_32rooo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   asl
e_33oo:
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
    sta 34*BLIT_STRIDE + blitRoll,x
    sta 35*BLIT_STRIDE + blitRoll,x
:   rts

e_32tooo:
    bmi :+
    sta 32*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_33oo

e_48rooo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   asl
e_49oo:
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
:   rts

e_48tooo:
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_49oo

e_52rooo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
e_53oo:
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
    sta 54*BLIT_STRIDE + blitRoll,x
    sta 55*BLIT_STRIDE + blitRoll,x
:   rts

e_52tooo:
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_53oo

e_56rooo:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   asl
e_57oo:
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
:   rts

e_56tooo:
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_57oo

e_64tooo:
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_65oo:
    bmi :+
    sta 65*BLIT_STRIDE + blitRoll,x
    sta 66*BLIT_STRIDE + blitRoll,x
    sta 67*BLIT_STRIDE + blitRoll,x
:   rts

e_t64ooo:
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   bvc e_65oo

e_68tooo:
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
e_69oo:
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
:   rts

e_r68ooo:
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   bvc e_69oo

e_72rooo:
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
e_73oo:
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
    sta 74*BLIT_STRIDE + blitRoll,x
    sta 75*BLIT_STRIDE + blitRoll,x
:   rts

e_t72ooo:
    iny
    lda (pTex),y
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   bvc e_73oo

e_92rooo:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
e_93oo:
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
    sta 94*BLIT_STRIDE + blitRoll,x
    sta 95*BLIT_STRIDE + blitRoll,x
:   rts

e_92tooo:
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bvc e_93oo

; Produce 2 rows from 2 rows
expand_2:
    jsr selectMip5
    iny
    lda (pTex),y
    bmi :+
    sta 63*BLIT_STRIDE + blitRoll,x
:
    asl
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:
expand_0:
    rts

; Produce 4 rows from 4 rows
expand_4:
    jsr selectMip4
    jsr e_t62ro
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 65*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 6 rows from 4 rows
expand_6:
    jsr selectMip4
    iny
    lda (pTex),y
    bmi :+
    sta 61*BLIT_STRIDE + blitRoll,x
:   jsr e_62ro
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
    sta 65*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 66*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 8 rows from 8 rows
expand_8:
    jsr selectMip3
    jsr e_t60rotoro
    jmp e_t64rotoro

; Produce 10 rows from 8 rows
expand_10:
    jsr selectMip3
    iny
    lda (pTex),y
    bmi :+
    sta 59*BLIT_STRIDE + blitRoll,x
:
    jsr e_60rotoro
    jsr e_t64oroto
    asl
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 12 rows from 8 rows
expand_12:
    jsr selectMip3
    jsr e_t58o
    jsr e_r60tooro
    jsr e_t64oroto
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 69*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 14 rows from 8 rows
expand_14:
    jsr selectMip3
    iny
    lda (pTex),y
    bmi :+
    sta 57*BLIT_STRIDE + blitRoll,x
:   jsr e_58ro
    jsr e_60tooro
    jsr e_t64oroo
    iny
    lda (pTex),y
    bmi :+
    sta 68*BLIT_STRIDE + blitRoll,x
    sta 69*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 70*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 16 rows from 16 rows
expand_16:
    jsr selectMip2
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64rotoro
    jmp e_t68rotoro

; Produce 18 rows from 16 rows
expand_18:
    jsr selectMip2
    iny
    lda (pTex),y
    bmi :+
    sta 55*BLIT_STRIDE + blitRoll,x
:
    jsr e_56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 20 rows from 16 rows
expand_20:
    jsr selectMip2
    jsr e_t54o
    jsr e_r56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68tooro
    iny
    lda (pTex),y
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 22 rows from 16 rows
expand_22:
    jsr selectMip2
    jsr e_t53oro
    jsr e_t56oroto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoro
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 73*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 74*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 24 rows from 16 rows
expand_24:
    jsr selectMip2
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroto
    jsr e_68rotoo
    jmp e_r72tooro

; Produce 26 rows from 16 rows
expand_26:
    jsr selectMip2
    iny
    lda (pTex),y
    bmi :+
    sta 51*BLIT_STRIDE + blitRoll,x
:
    jsr e_52rooto
    jsr e_r56otoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_72rotoo
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 28 rows from 16 rows
expand_28:
    jsr selectMip2
    jsr e_t50o
    jsr e_r52otoo
    jsr e_r56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 77*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 30 rows from 16 rows
expand_30:
    jsr selectMip2
    iny
    lda (pTex),y
    bmi :+
    sta 49*BLIT_STRIDE + blitRoll,x
:   jsr e_50ro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    iny
    lda (pTex),y
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 78*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 32 rows from 32 rows
expand_32:
    jsr selectMip1
    jsr e_t48rotoro
    jsr e_t52rotoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64rotoro
    jsr e_t68rotoro
    jsr e_t72rotoro
    jmp e_t76rotoro

; Produce 34 rows from 32 rows
expand_34:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 47*BLIT_STRIDE + blitRoll,x
:
    jsr e_48rotoro
    jsr e_t52rotoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72toroto
    jsr e_r76toroto
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 36 rows from 32 rows
expand_36:
    jsr selectMip1
    jsr e_t46o
    jsr e_r48toroto
    jsr e_r52toroto
    jsr e_56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72tooro
    jsr e_t76rotoro
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 38 rows from 32 rows
expand_38:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 45*BLIT_STRIDE + blitRoll,x
:   jsr e_46ro
    jsr e_t48rotoro
    jsr e_52toroto
    jsr e_r56tooro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroo
    jsr e_t72rotoro
    jsr e_t76oroto
    asl
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 82*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 40 rows from 32 rows
expand_40:
    jsr selectMip1
    jsr e_t44oroto
    jsr e_r48tooro
    jsr e_t52rotoo
    jsr e_r56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68tooro
    jsr e_t72rotoo
    jsr e_r76toroto
    jmp e_80rotoro

; Produce 42 rows from 32 rows
expand_42:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 43*BLIT_STRIDE + blitRoll,x
:
    jsr e_44rotoro
    jsr e_48toroto
    jsr e_52rotoro
    jsr e_56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68otoro
    jsr e_t72oroto
    jsr e_r76otoro
    jsr e_t80oroto
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 44 rows from 32 rows
expand_44:
    jsr selectMip1
    jsr e_t42o
    jsr e_r44tooro
    jsr e_t48rooto
    jsr e_r52tooro
    jsr e_t56oroto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoro
    jsr e_72toroto
    jsr e_76rotoo
    jsr e_r80toroo
    iny
    lda (pTex),y
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 46 rows from 32 rows
expand_46:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 41*BLIT_STRIDE + blitRoll,x
:   jsr e_42ro
    jsr e_t44oroto
    jsr e_48rotoo
    jsr e_r52toroo
    jsr e_t56rooto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoo
    jsr e_r72tooro
    jsr e_t76rooto
    jsr e_r80otoro
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 85*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 86*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 48 rows from 32 rows
expand_48:
    jsr selectMip1
    jsr e_t40oroto
    jsr e_44rotoo
    jsr e_r48tooro
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroto
    jsr e_68rotoo
    jsr e_r72tooro
    jsr e_t76oroto
    jsr e_80rotoo
    jmp e_r84tooro

; Produce 50 rows from 32 rows
expand_50:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 39*BLIT_STRIDE + blitRoll,x
:
    jsr e_40rooto
    jsr e_r44otoro
    jsr e_48toroo
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_r72otoro
    jsr e_76tooro
    jsr e_t80oroto
    jsr e_84rotoo
    asl
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 52 rows from 32 rows
expand_52:
    jsr selectMip1
    jsr e_t38o
    jsr e_r40otoro
    jsr e_44tooro
    jsr e_t48oroto
    jsr e_52rooto
    jsr e_r56otoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_72rotoo
    jsr e_r76tooro
    jsr e_80toroo
    jsr e_t84oroto
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 89*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 54 rows from 32 rows
expand_54:
    jsr selectMip1
    jsr e_t37oro
    jsr e_40tooro
    jsr e_t44oroo
    jsr e_t48rooto
    jsr e_52rotoo
    jsr e_r56otoro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    jsr e_r76otoo
    jsr e_r80tooro
    jsr e_84toroo
    iny
    lda (pTex),y
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 90*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 56 rows from 32 rows
expand_56:
    jsr selectMip1
    jsr e_t36oroo
    jsr e_t40oroto
    jsr e_44rooto
    jsr e_48rotoo
    jsr e_r52otoo
    jsr e_r56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    jsr e_76rotoo
    jsr e_r80otoo
    jsr e_r84tooro
    jmp e_88tooro

; Produce 58 rows from 32 rows
expand_58:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 35*BLIT_STRIDE + blitRoll,x
:
    jsr e_36rooto
    jsr e_40rooto
    jsr e_44rotoo
    jsr e_r48otoo
    jsr e_r52otoro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroto
    jsr e_76rooto
    jsr e_80rooto
    jsr e_r84otoo
    jsr e_r88otoo
    asl
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 60 rows from 32 rows
expand_60:
    jsr selectMip1
    jsr e_t34o
    jsr e_r36otoo
    jsr e_r40otoo
    jsr e_r44otoo
    jsr e_r48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroto
    jsr e_80rooto
    jsr e_84rooto
    jsr e_88rooto
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 93*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 62 rows from 32 rows
expand_62:
    jsr selectMip1
    iny
    lda (pTex),y
    bmi :+
    sta 33*BLIT_STRIDE + blitRoll,x
:   jsr e_34ro
    jsr e_36tooro
    jsr e_40tooro
    jsr e_44tooro
    jsr e_48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroo
    jsr e_t80oroo
    jsr e_t84oroo
    jsr e_t88oroo
    iny
    lda (pTex),y
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
    sta 93*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 94*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 64 rows from 64 rows
expand_64:
    jsr selectMip0
    jsr e_t32rotoro
    jsr e_t36rotoro
    jsr e_t40rotoro
    jsr e_t44rotoro
    jsr e_t48rotoro
    jsr e_t52rotoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64rotoro
    jsr e_t68rotoro
    jsr e_t72rotoro
    jsr e_t76rotoro
    jsr e_t80rotoro
    jsr e_t84rotoro
    jsr e_t88rotoro
    jmp e_t92rotoro

; Produce 66 rows from 64 rows
expand_66:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 31*BLIT_STRIDE + blitRoll,x
:
    jsr e_32rotoro
    jsr e_t36rotoro
    jsr e_t40rotoro
    jsr e_t44rotoro
    jsr e_t48rotoro
    jsr e_t52rotoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72toroto
    jsr e_r76toroto
    jsr e_r80toroto
    jsr e_r84toroto
    jsr e_r88toroto
    jsr e_r92toroto
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 68 rows from 64 rows
expand_68:
    jsr selectMip0
    jsr e_t30o
    jsr e_r32toroto
    jsr e_r36toroto
    jsr e_r40toroto
    jsr e_r44toroto
    jsr e_48rotoro
    jsr e_t52rotoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72toroto
    jsr e_r76toroto
    jsr e_r80tooro
    jsr e_t84rotoro
    jsr e_t88rotoro
    jsr e_t92rotoro
    iny
    lda (pTex),y
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 70 rows from 64 rows
expand_70:
    jsr selectMip0
    jsr e_t29oro
    jsr e_t32rotoro
    jsr e_t36rotoro
    jsr e_t40oroto
    jsr e_r44toroto
    jsr e_r48toroto
    jsr e_r52otoro
    jsr e_t56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72toroto
    jsr e_76rotoro
    jsr e_t80rotoro
    jsr e_t84rotoro
    jsr e_88toroto
    jsr e_r92toroto
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 97*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 98*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 72 rows from 64 rows
expand_72:
    jsr selectMip0
    jsr e_t28oroto
    jsr e_r32toroto
    jsr e_r36tooro
    jsr e_t40rotoro
    iny
    lda (pTex),y
    jsr e_44rotoo
    jsr e_r48toroto
    jsr e_r52toroto
    jsr e_56rotoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_r72tooro
    jsr e_t76rotoro
    jsr e_t80rotoo
    jsr e_r84toroto
    jsr e_r88toroto
    jsr e_92rotoro
    iny
    lda (pTex),y
    jsr e_96rotoro
    rts

; Produce 74 rows from 64 rows
expand_74:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 27*BLIT_STRIDE + blitRoll,x
:
    jsr e_28rotoro
    jsr e_t32rotoo
    jsr e_r36toroto
    jsr e_r40tooro
    jsr e_t44rotoro
    jsr e_t48rooto
    jsr e_r52toroto
    jsr e_r56otoro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroto
    jsr e_72rotoro
    iny
    lda (pTex),y
    jsr e_76rotoo
    jsr e_r80toroto
    jsr e_r84toroo
    jsr e_t88rotoro
    jsr e_t92rooto
    jsr e_r96toroto
    asl
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 76 rows from 64 rows
expand_76:
    jsr selectMip0
    jsr e_t26o
    asl
    jsr e_28toroto
    jsr e_r32otoro
    jsr e_t36rotoo
    jsr e_r40toroto
    jsr e_r44tooro
    jsr e_t48rotoro
    jsr e_52toroto
    jsr e_r56tooro
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68toroo
    jsr e_t72rotoro
    jsr e_t76oroto
    jsr e_r80toroto
    jsr e_84rotoro
    jsr e_t88rooto
    jsr e_r92toroto
    jsr e_96rotoro
    iny
    lda (pTex),y
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 78 rows from 64 rows
expand_78:
    jsr selectMip0
    jsr e_t25oro
    jsr e_t28rotoo
    jsr e_r32toroto
    jsr e_r36otoro
    jsr e_t40rooto
    jsr e_r44toroto
    jsr e_48rotoro
    jsr e_t52oroto
    asl
    bmi :+
    sta 56*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_57roo
    jsr e_t60rotoro
    jsr e_t64oroto
    jsr e_r68tooro
    jsr e_t72rotoro
    jsr e_76toroto
    jsr e_r80otoro
    jsr e_t84rotoo
    jsr e_r88toroto
    jsr e_92rotoro
    jsr e_t96rooto
    asl
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 101*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 102*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 80 rows from 64 rows
expand_80:
    jsr selectMip0
    jsr e_t24oroto
    jsr e_r28tooro
    jsr e_t32rotoo
    jsr e_r36toroto
    jsr e_40rotoro
    jsr e_t44oroto
    jsr e_r48tooro
    jsr e_t52rotoo
    jsr e_r56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68tooro
    jsr e_t72rotoo
    jsr e_r76toroto
    jsr e_80rotoro
    jsr e_t84oroto
    jsr e_r88tooro
    iny
    lda (pTex),y
    jsr e_92rotoo
    jsr e_r96toroto
    jmp e_100rotoro

; Produce 82 rows from 64 rows
expand_82:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 23*BLIT_STRIDE + blitRoll,x
:
    jsr e_24rotoro
    jsr e_28toroto
    jsr e_r32otoro
    jsr e_t36oroto
    jsr e_r40tooro
    jsr e_t44rooto
    asl
    jsr e_48toroo
    jsr e_t52rotoo
    jsr e_r56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68otoro
    jsr e_t72rooto
    jsr e_r76tooro
    jsr e_t80rotoo
    jsr e_r84toroo
    jsr e_t88rotoro
    jsr e_92toroto
    jsr e_96rotoro
    jsr e_t100oroto
    asl
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 84 rows from 64 rows
expand_84:
    jsr selectMip0
    jsr e_t22o
    asl
    jsr e_24toroo
    jsr e_t28rotoo
    asl
    jsr e_32toroo
    jsr e_t36rotoo
    jsr e_r40toroto
    jsr e_44rotoro
    jsr e_48toroto
    jsr e_52rotoro
    jsr e_56toroto
    jsr e_60rotoro
    jsr e_t64oroto
    jsr e_r68otoro
    jsr e_t72oroto
    jsr e_r76otoro
    jsr e_t80oroto
    jsr e_r84tooro
    jsr e_t88rooto
    jsr e_r92tooro
    jsr e_t96rooto
    jsr e_r100tooro
    iny
    lda (pTex),y
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 86 rows from 64 rows
expand_86:
    jsr selectMip0
    jsr e_t21oro
    jsr e_t24oroto
    asl
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_30ro
    jsr e_t32oroto
    jsr e_r36otoro
    jsr e_t40oroto
    jsr e_r44otoro
    jsr e_t48oroto
    jsr e_r52otoro
    jsr e_t56oroto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoro
    jsr e_72toroto
    jsr e_76rotoro
    jsr e_80toroto
    jsr e_84rotoro
    jsr e_88toroto
    jsr e_92rotoro
    jsr e_96toroto
    jsr e_100rotoro
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 105*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 106*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 88 rows from 64 rows
expand_88:
    jsr selectMip0
    jsr e_t20oroto
    jsr e_24rotoro
    jsr e_28toroto
    jsr e_32rotoo
    asl
    jsr e_36toroo
    iny
    lda (pTex),y
    jsr e_40rotoo
    jsr e_r44tooro
    jsr e_t48rooto
    jsr e_r52tooro
    jsr e_t56oroto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoro
    jsr e_72toroto
    jsr e_76rotoo
    jsr e_r80toroo
    jsr e_t84rotoo
    jsr e_r88tooro
    jsr e_t92rooto
    jsr e_r96tooro
    jsr e_t100oroto
    jmp e_r104otoro

; Produce 90 rows from 64 rows
expand_90:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 19*BLIT_STRIDE + blitRoll,x
:
    jsr e_20rotoo
    jsr e_r24tooro
    jsr e_t28rooto
    jsr e_r32otoro
    jsr e_t36oroto
    jsr e_40rotoro
    jsr e_44toroo
    iny
    lda (pTex),y
    jsr e_48rotoo
    jsr e_r52tooro
    jsr e_t56rooto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoo
    asl
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_73roo
    jsr e_t76rooto
    jsr e_r80tooro
    jsr e_t84oroto
    asl
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_90ro
    jsr e_92toroto
    jsr e_96rotoo
    asl
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_101roo
    jsr e_t104rooto
    asl
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 92 rows from 64 rows
expand_92:
    jsr selectMip0
    jsr e_t18o
    jsr e_r20tooro
    jsr e_t24oroto
    jsr e_28rotoro
    jsr e_32toroo
    jsr e_t36rooto
    jsr e_r40tooro
    jsr e_t44oroto
    jsr e_48rotoo
    jsr e_r52toroo
    jsr e_t56rooto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoo
    jsr e_r72tooro
    jsr e_t76rooto
    jsr e_r80otoro
    jsr e_84toroto
    jsr e_88rotoo
    jsr e_r92tooro
    jsr e_t96oroto
    jsr e_r100otoro
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_105roo
    iny
    lda (pTex),y
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 94 rows from 64 rows
expand_94:
    jsr selectMip0
    jsr e_t17oro
    jsr e_t20oroto
    jsr e_24rotoo
    jsr e_r28tooro
    jsr e_t32oroto
    jsr e_36rotoo
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_41roo
    jsr e_t44rooto
    asl
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
:   jsr e_49toro
    jsr e_52toroo
    jsr e_t56rooto
    jsr e_r60otoro
    jsr e_t64oroto
    jsr e_68rotoo
    jsr e_r72tooro
    jsr e_t76oroto
    jsr e_80rotoo
    jsr e_r84tooro
    jsr e_t88rooto
    jsr e_r92otoro
    jsr e_96toroo
    jsr e_t100rooto
    jsr e_r104otoro
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 109*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 110*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 96 rows from 64 rows
expand_96:
    jsr selectMip0
    jsr e_t16oroto
    jsr e_20rotoo
    jsr e_r24tooro
    jsr e_t28oroto
    jsr e_32rotoo
    jsr e_r36tooro
    jsr e_t40oroto
    jsr e_44rotoo
    jsr e_r48tooro
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroto
    jsr e_68rotoo
    jsr e_r72tooro
    jsr e_t76oroto
    jsr e_80rotoo
    jsr e_r84tooro
    jsr e_t88oroto
    jsr e_92rotoo
    jsr e_r96tooro
    jsr e_t100oroto
    jsr e_104rotoo
    jmp e_r108tooro

; Produce 98 rows from 64 rows
expand_98:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 15*BLIT_STRIDE + blitRoll,x
:
    jsr e_16rooto
    asl
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
    sta 21*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_22ro
    jsr e_24toroo
    jsr e_t28rooto
    jsr e_r32otoro
    jsr e_36toroo
    jsr e_t40oroto
    jsr e_44rotoo
    jsr e_r48tooro
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_r72otoro
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_77roo
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 81*BLIT_STRIDE + blitRoll,x
:   jsr e_82to
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   jsr e_85toro
    jsr e_88tooro
    jsr e_t92oroto
    jsr e_96rotoo
    jsr e_r100tooro
    iny
    lda (pTex),y
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
    sta 105*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_106to
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_109too
    asl
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 100 rows from 64 rows
expand_100:
    jsr selectMip0
    jsr e_t14o
    asl
    bmi :+
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_18ro
    jsr e_20toroo
    iny
    lda (pTex),y
    jsr e_24rooto
    jsr e_28rotoo
    jsr e_r32tooro
    jsr e_t36oroto
    jsr e_40rooto
    jsr e_r44otoro
    jsr e_48toroo
    jsr e_t52oroto
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_r72otoro
    jsr e_76tooro
    jsr e_t80oroto
    jsr e_84rotoo
    jsr e_r88tooro
    bmi :+
    sta 92*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_93roo
    jsr e_t96rooto
    jsr e_r100otoo
    asl
    jsr e_104tooro
    iny
    lda (pTex),y
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
    sta 109*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_110to
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 113*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 102 rows from 64 rows
expand_102:
    jsr selectMip0
    jsr e_t13oro
    jsr e_16toroo
    iny
    lda (pTex),y
    jsr e_20rooto
    jsr e_24rotoo
    jsr e_r28tooro
    jsr e_32toroo
    jsr e_t36rooto
    jsr e_40rotoo
    jsr e_r44tooro
    jsr e_48toroo
    iny
    lda (pTex),y
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 53*BLIT_STRIDE + blitRoll,x
:   jsr e_54to
    jsr e_56rotoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_r72otoo
    jsr e_r76tooro
    jsr e_t80oroo
    jsr e_t84rooto
    jsr e_r88otoo
    jsr e_r92tooro
    jsr e_t96oroo
    jsr e_t100rooto
    jsr e_r104otoo
    jsr e_r108tooro
    iny
    lda (pTex),y
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
    sta 113*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 114*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 104 rows from 64 rows
expand_104:
    jsr selectMip0
    jsr e_t12oroo
    iny
    lda (pTex),y
    jsr e_16rooto
    jsr e_20rotoo
    jsr e_r24tooro
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_29roo
    jsr e_t32oroto
    jsr e_36rotoo
    jsr e_r40otoro
    jsr e_44tooro
    jsr e_t48oroto
    jsr e_52rooto
    jsr e_r56otoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_72rotoo
    jsr e_r76tooro
    jsr e_80toroo
    jsr e_t84oroto
    jsr e_88rotoo
    jsr e_r92otoro
    jsr e_96tooro
    jsr e_t100oroto
    jsr e_104rooto
    jsr e_r108otoo
    jmp e_r112tooro

; Produce 106 rows from 64 rows
expand_106:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 11*BLIT_STRIDE + blitRoll,x
:
    jsr e_12rooto
    jsr e_r16otoo
    jsr e_r20tooro
    jsr e_24toroo
    jsr e_t28oroto
    jsr e_32rooto
    jsr e_r36otoo
    jsr e_r40tooro
    jsr e_44toroo
    jsr e_t48oroto
    jsr e_52rooto
    jsr e_r56otoo
    jsr e_r60tooro
    jsr e_t64oroo
    jsr e_t68rooto
    jsr e_72rotoo
    jsr e_r76otoro
    jsr e_80tooro
    jsr e_t84oroo
    jsr e_t88rooto
    jsr e_92rotoo
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   jsr e_97toro
    jsr e_100tooro
    jsr e_t104oroo
    iny
    lda (pTex),y
    jsr e_108rooto
    jsr e_112rotoo
    asl
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 108 rows from 64 rows
expand_108:
    jsr selectMip0
    jsr e_t10o
    jsr e_r12otoo
    jsr e_r16tooro
    jsr e_20toroo
    jsr e_t24oroto
    jsr e_28rooto
    jsr e_r32otoo
    jsr e_r36tooro
    jsr e_40tooro
    jsr e_t44oroo
    jsr e_t48rooto
    jsr e_52rotoo
    jsr e_r56otoro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    jsr e_r76otoo
    jsr e_r80tooro
    jsr e_84toroo
    jsr e_t88oroto
    jsr e_92rooto
    jsr e_96rotoo
    jsr e_r100otoro
    jsr e_104tooro
    jsr e_t108oroo
    jsr e_t112rooto
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 117*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 110 rows from 64 rows
expand_110:
    jsr selectMip0
    jsr e_t9oro
    jsr e_12tooro
    jsr e_t16oroo
    jsr e_t20oroto
    jsr e_24rooto
    jsr e_r28otoo
    jsr e_r32otoro
    jsr e_36tooro
    jsr e_t40oroo
    jsr e_t44oroto
    jsr e_48rooto
    jsr e_r52otoo
    jsr e_r56otoro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    jsr e_76rotoo
    jsr e_r80otoro
    jsr e_84tooro
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_89roo
    jsr e_t92oroto
    jsr e_96rooto
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_101too
    jsr e_r104otoro
    jsr e_108tooro
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_113roo
    iny
    lda (pTex),y
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 118*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 112 rows from 64 rows
expand_112:
    jsr selectMip0
    jsr e_t8oroo
    iny
    lda (pTex),y
    bmi :+
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_14to
    jsr e_16rooto
    jsr e_20rotoo
    jsr e_r24otoo
    jsr e_r28tooro
    jsr e_32tooro
    jsr e_t36oroo
    jsr e_t40oroto
    jsr e_44rooto
    jsr e_48rotoo
    jsr e_r52otoo
    jsr e_r56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroto
    jsr e_72rooto
    jsr e_76rotoo
    jsr e_r80otoo
    jsr e_r84tooro
    jsr e_88tooro
    jsr e_t92oroo
    jsr e_t96oroto
    jsr e_100rooto
    jsr e_104rotoo
    jsr e_r108otoo
    jsr e_r112tooro
    jmp e_116tooro

; Produce 114 rows from 64 rows
expand_114:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 7*BLIT_STRIDE + blitRoll,x
:
    jsr e_8rooto
    jsr e_12rooto
    jsr e_r16otoo
    jsr e_r20otoo
    jsr e_r24tooro
    jsr e_28tooro
    jsr e_t32oroo
    jsr e_t36oroo
    jsr e_t40rooto
    jsr e_44rooto
    jsr e_r48otoo
    jsr e_r52otoo
    jsr e_r56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72rooto
    jsr e_76rooto
    jsr e_80rotoo
    jsr e_r84otoo
    jsr e_r88tooro
    jsr e_92tooro
    jsr e_96toroo
    jsr e_t100oroo
    jsr e_t104rooto
    jsr e_108rooto
    jsr e_112rotoo
    jsr e_r116otoo
    asl
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 116 rows from 64 rows
expand_116:
    jsr selectMip0
    jsr e_t6o
    jsr e_r8otoo
    jsr e_r12otoo
    jsr e_r16tooro
    jsr e_20tooro
    jsr e_24toroo
    jsr e_t28oroo
    jsr e_t32oroto
    jsr e_36rooto
    jsr e_40rooto
    jsr e_44rotoo
    jsr e_r48otoo
    jsr e_r52otoro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroto
    jsr e_76rooto
    jsr e_80rooto
    jsr e_r84otoo
    jsr e_r88otoo
    jsr e_r92tooro
    jsr e_96tooro
    jsr e_100tooro
    jsr e_t104oroo
    jsr e_t108oroo
    jsr e_t112rooto
    jsr e_116rooto
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 121*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 118 rows from 64 rows
expand_118:
    jsr selectMip0
    jsr e_t5oro
    jsr e_8tooro
    jsr e_12tooro
    jsr e_16toroo
    jsr e_t20oroo
    jsr e_t24oroo
    jsr e_t28rooto
    jsr e_32rooto
    jsr e_36rooto
    jsr e_40rotoo
    jsr e_r44otoo
    jsr e_r48otoo
    jsr e_r52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76rooto
    jsr e_80rooto
    jsr e_84rooto
    jsr e_r88otoo
    jsr e_r92otoo
    jsr e_r96otoo
    jsr e_r100tooro
    jsr e_104tooro
    jsr e_108tooro
    jsr e_t112oroo
    jsr e_t116oroo
    iny
    lda (pTex),y
    bmi :+
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 122*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 120 rows from 64 rows
expand_120:
    jsr selectMip0
    jsr e_t4oroo
    jsr e_t8oroo
    jsr e_t12oroo
    jsr e_t16oroto
    jsr e_20rooto
    jsr e_24rooto
    jsr e_28rooto
    jsr e_32rotoo
    jsr e_r36otoo
    jsr e_r40otoo
    jsr e_r44otoo
    jsr e_r48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroto
    jsr e_80rooto
    jsr e_84rooto
    jsr e_88rooto
    jsr e_92rotoo
    jsr e_r96otoo
    jsr e_r100otoo
    jsr e_r104otoo
    jsr e_r108tooro
    jsr e_112tooro
    jsr e_116tooro
    jmp e_120tooro

; Produce 122 rows from 64 rows
expand_122:
    jsr selectMip0
    iny
    lda (pTex),y
    bmi :+
    sta 3*BLIT_STRIDE + blitRoll,x
:
    jsr e_4rooto
    jsr e_8rooto
    jsr e_12rooto
    jsr e_16rooto
    jsr e_20rooto
    jsr e_r24otoo
    jsr e_r28otoo
    jsr e_r32otoo
    jsr e_r36otoo
    jsr e_r40otoo
    jsr e_r44tooro
    jsr e_48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroo
    jsr e_t80oroo
    jsr e_t84rooto
    jsr e_88rooto
    jsr e_92rooto
    jsr e_96rooto
    jsr e_100rooto
    jsr e_104rotoo
    jsr e_r108otoo
    jsr e_r112otoo
    jsr e_r116otoo
    jsr e_r120otoo
    asl
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 124 rows from 64 rows
expand_124:
    jsr selectMip0
    jsr e_t2o
    jsr e_r4otoo
    jsr e_r8otoo
    jsr e_r12otoo
    jsr e_r16otoo
    jsr e_r20otoo
    jsr e_r24otoo
    jsr e_r28otoo
    jsr e_r32tooro
    jsr e_36tooro
    jsr e_40tooro
    jsr e_44tooro
    jsr e_48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroo
    jsr e_t80oroo
    jsr e_t84oroo
    jsr e_t88oroo
    jsr e_t92oroto
    jsr e_96rooto
    jsr e_100rooto
    jsr e_104rooto
    jsr e_108rooto
    jsr e_112rooto
    jsr e_116rooto
    jsr e_120rooto
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 125*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 126 rows from 64 rows
expand_126:
    jsr selectMip0
    jsr e_t1oro
    jsr e_4tooro
    jsr e_8tooro
    jsr e_12tooro
    jsr e_16tooro
    jsr e_20tooro
    jsr e_24tooro
    jsr e_28tooro
    jsr e_32tooro
    jsr e_36tooro
    jsr e_40tooro
    jsr e_44tooro
    jsr e_48tooro
    jsr e_52tooro
    jsr e_56tooro
    jsr e_60tooro
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroo
    jsr e_t80oroo
    jsr e_t84oroo
    jsr e_t88oroo
    jsr e_t92oroo
    jsr e_t96oroo
    jsr e_t100oroo
    jsr e_t104oroo
    jsr e_t108oroo
    jsr e_t112oroo
    jsr e_t116oroo
    jsr e_t120oroo
    iny
    lda (pTex),y
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
    sta 125*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 126*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 128 rows from 64 rows
expand_128:
    jsr selectMip0
    jsr e_t0oroo
    jsr e_t4oroo
    jsr e_t8oroo
    jsr e_t12oroo
    jsr e_t16oroo
    jsr e_t20oroo
    jsr e_t24oroo
    jsr e_t28oroo
    jsr e_t32oroo
    jsr e_t36oroo
    jsr e_t40oroo
    jsr e_t44oroo
    jsr e_t48oroo
    jsr e_t52oroo
    jsr e_t56oroo
    jsr e_t60oroo
    jsr e_t64oroo
    jsr e_t68oroo
    jsr e_t72oroo
    jsr e_t76oroo
    jsr e_t80oroo
    jsr e_t84oroo
    jsr e_t88oroo
    jsr e_t92oroo
    jsr e_t96oroo
    jsr e_t100oroo
    jsr e_t104oroo
    jsr e_t108oroo
    jsr e_t112oroo
    jsr e_t116oroo
    jsr e_t120oroo
    jmp e_t124oroo

; Produce 132 rows from 64 rows
expand_132:
    jsr selectMip0
    jsr e_t0rooto
    jsr e_4rooto
    jsr e_8rooto
    jsr e_12rooto
    jsr e_16rooto
    jsr e_20rooto
    jsr e_24rooto
    jsr e_28rooto
    jsr e_32oroo
    jsr e_t36oroo
    jsr e_t40oroo
    jsr e_t44oroo
    jsr e_t48oroo
    jsr e_t52oroo
    jsr e_t56oroo
    jsr e_t60oroo
    jsr e_t64ooro
    jsr e_68tooro
    jsr e_72tooro
    jsr e_76tooro
    jsr e_80tooro
    jsr e_84tooro
    jsr e_88tooro
    jsr e_92tooro
    jsr e_96tooo
    jsr e_r100otoo
    jsr e_r104otoo
    jsr e_r108otoo
    jsr e_r112otoo
    jsr e_r116otoo
    jsr e_r120otoo
    jmp e_r124otoo

; Produce 136 rows from 64 rows
expand_136:
    jsr selectMip0
    iny
    lda (pTex),y
    asl
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   jsr e_t1oro
    jsr e_4tooro
    jsr e_8tooro
    jsr e_12tooo
    jsr e_r16otoo
    jsr e_r20otoo
    jsr e_r24otoo
    jsr e_r28otoo
    jsr e_32rooto
    jsr e_36rooto
    jsr e_40rooto
    jsr e_44rooto
    jsr e_48oroo
    jsr e_t52oroo
    jsr e_t56oroo
    jsr e_t60oroo
    jsr e_t64ooro
    jsr e_68tooro
    jsr e_72tooro
    jsr e_76tooro
    jsr e_80tooo
    jsr e_r84otoo
    jsr e_r88otoo
    jsr e_r92otoo
    jsr e_r96otoo
    jsr e_100rooto
    jsr e_104rooto
    jsr e_108rooto
    jsr e_112rooto
    jsr e_116oroo
    jsr e_t120oroo
    jmp e_t124oroo

; Produce 140 rows from 64 rows
expand_140:
    jsr selectMip0
    ldy #0
    jsr e_t0rooto
    jsr e_4rooo
    jsr e_t8oroo
    jsr e_t12oroo
    jsr e_t16ooro
    jsr e_20tooro
    jsr e_24tooro
    jsr e_28tooo
    jsr e_r32otoo
    jsr e_r36otoo
    jsr e_r40ooto
    jsr e_44rooto
    jsr e_48rooto
    jsr e_52oroo
    jsr e_t56oroo
    jsr e_t60oroo
    jsr e_t64ooro
    jsr e_68tooro
    jsr e_72tooro
    jsr e_76otoo
    jsr e_r80otoo
    jsr e_r84otoo
    jsr e_88rooto
    jsr e_92rooto
    jsr e_96rooto
    jsr e_100oroo
    jsr e_t104oroo
    jsr e_t108oroo
    jsr e_112tooro
    jsr e_116tooro
    jsr e_120tooo
    jmp e_r124otoo

; Produce 144 rows from 64 rows
expand_144:
    jsr selectMip0
    ldy #0
    jsr e_tr0otoo
    jsr e_r4otoo
    jsr e_r8ooto
    jsr e_12rooto
    jsr e_16rooo
    jsr e_t20oroo
    jsr e_t24oroo
    jsr e_28tooro
    jsr e_32tooro
    jsr e_36otoo
    jsr e_r40otoo
    jsr e_r44ooto
    jsr e_48rooto
    jsr e_52rooo
    jsr e_t56oroo
    jsr e_t60oroo
    jsr e_64tooro
    jsr e_68tooro
    jsr e_72otoo
    jsr e_r76otoo
    jsr e_r80ooto
    jsr e_84rooto
    jsr e_88rooo
    jsr e_t92oroo
    jsr e_t96oroo
    jsr e_100tooro
    jsr e_104tooro
    jsr e_108otoo
    jsr e_r112otoo
    jsr e_r116ooto
    jsr e_120rooto
    jmp e_124rooo

; Produce 148 rows from 64 rows
expand_148:
    jsr selectMip0
    ldy #1
    jsr e_t0oroo
    jsr e_t4ooro
    jsr e_8tooro
    jsr e_12otoo
    jsr e_r16otoo
    jsr e_20rooto
    jsr e_24rooto
    jsr e_28oroo
    jsr e_t32oroo
    jsr e_36tooro
    jsr e_40tooo
    jsr e_r44otoo
    jsr e_r48ooto
    jsr e_52rooto
    jsr e_56oroo
    jsr e_t60oroo
    jsr e_t64ooro
    jsr e_68tooro
    jsr e_72otoo
    jsr e_r76otoo
    jsr e_80rooto
    jsr e_84rooo
    jsr e_t88oroo
    jsr e_t92ooro
    jsr e_96tooro
    jsr e_100tooo
    jsr e_r104otoo
    jsr e_r108ooto
    jsr e_112rooto
    jsr e_116oroo
    jsr e_t120oroo
    jmp e_124tooro

; Produce 152 rows from 64 rows
expand_152:
    jsr selectMip0
    ldy #1
    jsr e_tr0ooto
    jsr e_4rooo
    jsr e_t8oroo
    jsr e_t12ooro
    jsr e_16tooro
    jsr e_20otoo
    jsr e_r24ooto
    jsr e_28rooto
    jsr e_32oroo
    jsr e_t36oroo
    jsr e_40tooro
    jsr e_44otoo
    jsr e_r48otoo
    jsr e_52rooto
    jsr e_56rooo
    jsr e_t60oroo
    jsr e_64tooro
    jsr e_68tooo
    jsr e_r72otoo
    jsr e_r76ooto
    jsr e_80rooo
    jsr e_t84oroo
    jsr e_t88ooro
    jsr e_92tooro
    jsr e_96otoo
    jsr e_r100ooto
    jsr e_104rooto
    jsr e_108oroo
    jsr e_t112oroo
    jsr e_116tooro
    jsr e_120otoo
    jmp e_r124otoo

; Produce 156 rows from 64 rows
expand_156:
    jsr selectMip0
    ldy #1
    jsr e_tr0tooo
    jsr e_r4otoo
    jsr e_r8ooto
    jsr e_12rooo
    jsr e_t16oroo
    jsr e_20tooro
    jsr e_24tooo
    jsr e_r28otoo
    jsr e_32rooto
    jsr e_36oroo
    jsr e_t40ooro
    jsr e_44tooro
    jsr e_48otoo
    asl
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
    sta 53*BLIT_STRIDE + blitRoll,x
:   jsr e_54to
    jsr e_56rooo
    jsr e_t60oroo
    jsr e_t64ooro
    jsr e_68tooo
    jsr e_r72otoo
    jsr e_76rooto
    jsr e_80oroo
    jsr e_t84oroo
    jsr e_88tooro
    jsr e_92otoo
    jsr e_r96ooto
    jsr e_100rooto
    jsr e_104oroo
    jsr e_t108ooro
    jsr e_112tooo
    jsr e_r116otoo
    jsr e_120rooto
    jmp e_124rooo

; Produce 160 rows from 64 rows
expand_160:
    jsr selectMip0
    ldy #2
    jsr e_t0oroo
    jsr e_t4ooro
    jsr e_8tooo
    jsr e_r12otoo
    jsr e_16rooto
    jsr e_20oroo
    jsr e_t24ooro
    jsr e_28tooo
    jsr e_r32otoo
    jsr e_36rooto
    jsr e_40oroo
    jsr e_t44ooro
    jsr e_48tooo
    jsr e_r52otoo
    jsr e_56rooto
    jsr e_60oroo
    jsr e_t64ooro
    jsr e_68tooo
    jsr e_r72otoo
    jsr e_76rooto
    jsr e_80oroo
    jsr e_t84ooro
    jsr e_88tooo
    jsr e_r92otoo
    jsr e_96rooto
    jsr e_100oroo
    jsr e_t104ooro
    jsr e_108tooo
    jsr e_r112otoo
    jsr e_116rooto
    jsr e_120oroo
    iny
    lda (pTex),y
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
:   jsr e_125oro
    rts

; Produce 164 rows from 64 rows
expand_164:
    jsr selectMip0
    ldy #2
    jsr e_tr0ooto
    jsr e_4oroo
    jsr e_t8ooro
    jsr e_12tooo
    jsr e_r16otoo
    jsr e_20rooto
    jsr e_24oroo
    jsr e_28tooro
    jsr e_32otoo
    asl
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   jsr e_37oto
    jsr e_40rooo
    jsr e_t44ooro
    jsr e_48tooo
    jsr e_r52otoo
    jsr e_56rooto
    jsr e_60oroo
    jsr e_t64ooro
    jsr e_68otoo
    jsr e_r72ooto
    jsr e_76rooo
    jsr e_t80oroo
    jsr e_84tooo
    jsr e_r88otoo
    jsr e_92rooto
    jsr e_96oroo
    jsr e_t100ooro
    jsr e_104tooo
    jsr e_r108ooto
    jsr e_112rooo
    jsr e_t116oroo
    jsr e_120tooro
    jmp e_124otoo

; Produce 168 rows from 64 rows
expand_168:
    jsr selectMip0
    ldy #2
    jsr e_tr0otoo
    jsr e_r4ooto
    jsr e_8rooo
    jsr e_t12ooro
    jsr e_16tooo
    jsr e_r20ooto
    jsr e_24rooo
    jsr e_t28oroo
    jsr e_32tooo
    jsr e_r36otoo
    jsr e_40rooo
    jsr e_t44oroo
    jsr e_48tooro
    jsr e_52otoo
    jsr e_56rooto
    jsr e_60oroo
    jsr e_64tooro
    jsr e_68otoo
    jsr e_r72ooto
    jsr e_76oroo
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
:   jsr e_82ro
    jsr e_84otoo
    asl
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
:   jsr e_90to
    jsr e_92rooo
    jsr e_t96ooro
    jsr e_100tooo
    jsr e_r104ooto
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   asl
    jsr e_109oo
    jsr e_t112oroo
    jsr e_116tooo
    jsr e_r120otoo
    jmp e_124rooo

; Produce 172 rows from 64 rows
expand_172:
    jsr selectMip0
    ldy #3
    jsr e_t0ooro
    jsr e_4tooo
    jsr e_r8ooto
    jsr e_12rooo
    jsr e_t16ooro
    jsr e_20tooo
    jsr e_r24ooto
    jsr e_28oroo
    jsr e_t32ooro
    jsr e_36otoo
    jsr e_r40ooto
    jsr e_44oroo
    jsr e_t48ooro
    jsr e_52otoo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_t64ooro
    jsr e_68otoo
    jsr e_72rooto
    jsr e_76oroo
    jsr e_80tooro
    jsr e_84otoo
    jsr e_88rooto
    jsr e_92oroo
    jsr e_96tooro
    jsr e_100otoo
    jsr e_104rooto
    jsr e_108oroo
    jsr e_112tooo
    jsr e_r116otoo
    jsr e_120rooo
    jmp e_t124oroo

; Produce 176 rows from 64 rows
expand_176:
    jsr selectMip0
    ldy #3
    jsr e_t0rooo
    jsr e_t4ooro
    jsr e_8otoo
    asl
    jsr e_12ooto
    jsr e_16oroo
    jsr e_20tooro
    jsr e_24otoo
    jsr e_28rooo
    jsr e_t32oroo
    jsr e_36tooo
    jsr e_r40ooto
    jsr e_44rooo
    jsr e_t48ooro
    jsr e_52otoo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_64tooro
    jsr e_68otoo
    jsr e_72rooo
    jsr e_t76oroo
    jsr e_80tooo
    jsr e_r84ooto
    jsr e_88rooo
    jsr e_t92ooro
    jsr e_96otoo
    jsr e_r100ooto
    jsr e_104oroo
    jsr e_108tooro
    jsr e_112otoo
    jsr e_116rooo
    jsr e_t120oroo
    jmp e_124tooo

; Produce 180 rows from 64 rows
expand_180:
    jsr selectMip0
    ldy #3
    jsr e_tr0ooto
    jsr e_4rooo
    jsr e_t8ooro
    jsr e_12otoo
    jsr e_16rooo
    jsr e_t20oroo
    jsr e_24tooo
    asl
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
:   jsr e_30to
    jsr e_32oroo
    jsr e_t36ooro
    jsr e_40otoo
    jsr e_44rooo
    jsr e_t48oroo
    jsr e_52tooo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_64tooro
    jsr e_68otoo
    jsr e_72rooo
    jsr e_t76ooro
    jsr e_80tooo
    jsr e_r84ooto
    jsr e_88oroo
    jsr e_92tooro
    jsr e_96otoo
    jsr e_100rooo
    jsr e_t104ooro
    jsr e_108otoo
    jsr e_r112ooto
    jsr e_116oroo
    jsr e_120tooo
    jmp e_r124otoo

; Produce 184 rows from 64 rows
expand_184:
    jsr selectMip0
    ldy #3
    jsr e_tr0tooo
    jsr e_r4ooto
    jsr e_8oroo
    jsr e_12tooo
    jsr e_r16otoo
    jsr e_20rooo
    jsr e_t24ooro
    jsr e_28otoo
    jsr e_32rooo
    jsr e_t36ooro
    jsr e_40tooo
    jsr e_r44ooto
    jsr e_48oroo
    jsr e_52tooo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_t64ooro
    jsr e_68otoo
    jsr e_72rooo
    jsr e_t76ooro
    jsr e_80otoo
    jsr e_84rooto
    jsr e_88oroo
    jsr e_92tooo
    jsr e_r96ooto
    jsr e_100oroo
    jsr e_104tooo
    jsr e_r108otoo
    jsr e_112rooo
    iny
    lda (pTex),y
    jsr e_116ooro
    jsr e_120otoo
    jmp e_124rooo

; Produce 188 rows from 64 rows
expand_188:
    jsr selectMip0
    ldy #4
    jsr e_t0ooro
    jsr e_4otoo
    jsr e_8rooo
    jsr e_t12ooro
    jsr e_16tooo
    jsr e_r20ooto
    jsr e_24oroo
    jsr e_28tooo
    jsr e_r32ooto
    jsr e_36oroo
    jsr e_40tooo
    jsr e_r44ooto
    jsr e_48oroo
    jsr e_52tooo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_t64ooro
    jsr e_68otoo
    jsr e_72rooo
    jsr e_t76ooro
    jsr e_80otoo
    jsr e_84rooo
    jsr e_t88ooro
    jsr e_92otoo
    jsr e_96rooo
    jsr e_t100ooro
    jsr e_104otoo
    jsr e_108rooto
    jsr e_112oroo
    jsr e_116tooo
    jsr e_r120ooto
    jmp e_124oroo

; Produce 192 rows from 64 rows
expand_192:
    jsr selectMip0
    ldy #4
    jsr e_t0oroo
    jsr e_4tooo
    jsr e_r8ooto
    jsr e_12oroo
    jsr e_16tooo
    jsr e_r20ooto
    jsr e_24oroo
    jsr e_28tooo
    jsr e_r32ooto
    jsr e_36oroo
    jsr e_40tooo
    jsr e_r44ooto
    jsr e_48oroo
    jsr e_52tooo
    jsr e_r56ooto
    jsr e_60oroo
    jsr e_64tooo
    jsr e_r68ooto
    jsr e_72oroo
    jsr e_76tooo
    jsr e_r80ooto
    jsr e_84oroo
    jsr e_88tooo
    jsr e_r92ooto
    jsr e_96oroo
    jsr e_100tooo
    jsr e_r104ooto
    jsr e_108oroo
    jsr e_112tooo
    jsr e_r116ooto
    jsr e_120oroo
    jmp e_124tooo

; Produce 200 rows from 64 rows
expand_200:
    jsr selectMip0
    ldy #4
    jsr e_tr0otoo
    jsr e_4rooo
    jsr e_t8ooro
    jsr e_12ooto
    jsr e_16oroo
    jsr e_20tooo
    jsr e_r24ooto
    jsr e_28oroo
    jsr e_32tooo
    asl
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   jsr e_37oo
    jsr e_t40ooro
    jsr e_44otoo
    jsr e_48rooo
    iny
    lda (pTex),y
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   jsr e_53oro
    jsr e_56otoo
    jsr e_60rooo
    jsr e_64tooo
    jsr e_r68ooto
    jsr e_72oroo
    jsr e_76tooo
    jsr e_r80ooto
    jsr e_84oroo
    jsr e_88otoo
    jsr e_92rooo
    jsr e_t96ooro
    jsr e_100otoo
    jsr e_104rooo
    jsr e_t108ooro
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   jsr e_113oto
    jsr e_116oroo
    jsr e_120tooo
    asl
    bmi :+
    sta 124*BLIT_STRIDE + blitRoll,x
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 127*BLIT_STRIDE + blitRoll,x
:
    rts

; Produce 208 rows from 64 rows
expand_208:
    jsr selectMip0
    ldy #5
    jsr e_t0ooro
    jsr e_4otoo
    jsr e_8rooo
    jsr e_t12ooo
    jsr e_r16ooto
    jsr e_20oroo
    jsr e_24tooo
    jsr e_28rooo
    jsr e_t32ooro
    jsr e_36otoo
    jsr e_40oroo
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    jsr e_45oo
    jsr e_r48ooto
    bmi :+
    sta 52*BLIT_STRIDE + blitRoll,x
:   jsr e_53oro
    jsr e_56otoo
    jsr e_60rooo
    jsr e_t64ooo
    jsr e_r68ooto
    jsr e_72oroo
    jsr e_76tooo
    jsr e_80rooo
    jsr e_t84ooro
    jsr e_88otoo
    jsr e_92oroo
    jsr e_96tooo
    jsr e_r100ooto
    jsr e_104ooro
    jsr e_108otoo
    jsr e_112rooo
    iny
    lda (pTex),y
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
:   jsr e_117oo
    jsr e_r120ooto
    jmp e_124oroo

; Produce 216 rows from 64 rows
expand_216:
    jsr selectMip0
    ldy #5
    iny
    lda (pTex),y
    asl
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
:   jsr e_1oo
    jsr e_t4ooro
    jsr e_8ooto
    jsr e_12oroo
    jsr e_16tooo
    jsr e_20rooo
    jsr e_t24ooro
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
:   jsr e_30to
    jsr e_32oroo
    jsr e_36otoo
    jsr e_40rooo
    iny
    lda (pTex),y
    bmi :+
    sta 44*BLIT_STRIDE + blitRoll,x
:   jsr e_45oo
    jsr e_r48ooto
    jsr e_52oroo
    jsr e_56otoo
    jsr e_60rooo
    jsr e_64tooo
    jsr e_r68ooto
    bmi :+
    sta 72*BLIT_STRIDE + blitRoll,x
    sta 73*BLIT_STRIDE + blitRoll,x
:   jsr e_74ro
    jsr e_76otoo
    jsr e_80rooo
    jsr e_84tooo
    jsr e_r88ooo
    jsr e_t92ooro
    jsr e_96otoo
    jsr e_100oroo
    jsr e_104tooo
    jsr e_r108ooo
    iny
    lda (pTex),y
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   jsr e_113oro
    jsr e_116ooto
    jsr e_120oroo
    jmp e_124tooo

; Produce 224 rows from 64 rows
expand_224:
    jsr selectMip0
    ldy #5
    jsr e_tr0otoo
    jsr e_4rooo
    jsr e_8tooo
    jsr e_r12ooo
    jsr e_t16ooro
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   jsr e_21oto
    jsr e_24oroo
    jsr e_28otoo
    jsr e_32rooo
    jsr e_36tooo
    asl
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   jsr e_41oo
    jsr e_t44ooro
    bmi :+
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
:   jsr e_50to
    jsr e_52oroo
    jsr e_56otoo
    jsr e_60rooo
    jsr e_64tooo
    jsr e_r68ooo
    jsr e_t72ooro
    jsr e_76ooto
    jsr e_80oroo
    jsr e_84otoo
    jsr e_88rooo
    jsr e_92tooo
    asl
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
:   jsr e_97oo
    jsr e_t100ooro
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   jsr e_105oto
    jsr e_108oroo
    jsr e_112otoo
    jsr e_116rooo
    jsr e_120tooo
    jmp e_r124ooo

; Produce 232 rows from 64 rows
expand_232:
    jsr selectMip0
    ldy #6
    jsr e_t0ooro
    jsr e_4otoo
    jsr e_8oroo
    jsr e_12otoo
    jsr e_16rooo
    jsr e_20tooo
    jsr e_24rooo
    iny
    lda (pTex),y
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   jsr e_29oo
    jsr e_r32ooto
    bmi :+
    sta 36*BLIT_STRIDE + blitRoll,x
:   jsr e_37oro
    bmi :+
    sta 40*BLIT_STRIDE + blitRoll,x
:   jsr e_41oto
    jsr e_44oroo
    jsr e_48otoo
    jsr e_52oroo
    jsr e_56tooo
    jsr e_60rooo
    jsr e_t64ooo
    jsr e_r68ooo
    jsr e_t72ooro
    jsr e_76ooto
    jsr e_80ooro
    jsr e_84otoo
    jsr e_88oroo
    jsr e_92tooo
    jsr e_96rooo
    jsr e_100tooo
    asl
    bmi :+
    sta 104*BLIT_STRIDE + blitRoll,x
:   jsr e_105oo
    iny
    lda (pTex),y
    jsr e_108ooo
    jsr e_r112ooto
    jsr e_116ooro
    jsr e_120otoo
    jmp e_124oroo

; Produce 240 rows from 64 rows
expand_240:
    jsr selectMip0
    ldy #6
    jsr e_t0rooo
    iny
    lda (pTex),y
    bmi :+
    sta 4*BLIT_STRIDE + blitRoll,x
:   jsr e_5oo
    asl
    bmi :+
    sta 8*BLIT_STRIDE + blitRoll,x
:   jsr e_9oo
    jsr e_t12ooo
    jsr e_r16ooto
    bmi :+
    sta 20*BLIT_STRIDE + blitRoll,x
:   jsr e_21oro
    bmi :+
    sta 24*BLIT_STRIDE + blitRoll,x
:   jsr e_25oto
    bmi :+
    sta 28*BLIT_STRIDE + blitRoll,x
:   jsr e_29oro
    jsr e_32otoo
    jsr e_36oroo
    jsr e_40otoo
    jsr e_44oroo
    jsr e_48tooo
    jsr e_52rooo
    jsr e_56tooo
    jsr e_60rooo
    jsr e_t64ooo
    jsr e_r68ooo
    jsr e_t72ooo
    jsr e_r76ooto
    jsr e_80ooro
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
    sta 85*BLIT_STRIDE + blitRoll,x
:   jsr e_86to
    bmi :+
    sta 88*BLIT_STRIDE + blitRoll,x
:   jsr e_89oro
    jsr e_92otoo
    jsr e_96oroo
    jsr e_100otoo
    jsr e_104oroo
    jsr e_108tooo
    jsr e_112rooo
    jsr e_116tooo
    jsr e_120rooo
    iny
    lda (pTex),y
    jsr e_124ooo
    rts

; Produce 248 rows from 64 rows
expand_248:
    jsr selectMip0
    ldy #6
    jsr e_tr0otoo
    jsr e_4oroo
    jsr e_8otoo
    jsr e_12oroo
    jsr e_16otoo
    jsr e_20oroo
    jsr e_24otoo
    jsr e_28oroo
    jsr e_32tooo
    jsr e_36rooo
    jsr e_40tooo
    jsr e_44rooo
    jsr e_48tooo
    jsr e_52rooo
    jsr e_56tooo
    jsr e_60rooo
    jsr e_t64ooo
    jsr e_r68ooo
    jsr e_t72ooo
    asl
    bmi :+
    sta 76*BLIT_STRIDE + blitRoll,x
:   jsr e_77oo
    iny
    lda (pTex),y
    bmi :+
    sta 80*BLIT_STRIDE + blitRoll,x
:   jsr e_81oo
    asl
    bmi :+
    sta 84*BLIT_STRIDE + blitRoll,x
:   jsr e_85oo
    iny
    lda (pTex),y
    jsr e_88ooo
    jsr e_r92ooto
    bmi :+
    sta 96*BLIT_STRIDE + blitRoll,x
    sta 97*BLIT_STRIDE + blitRoll,x
:   jsr e_98ro
    bmi :+
    sta 100*BLIT_STRIDE + blitRoll,x
:   jsr e_101oto
    jsr e_104ooro
    bmi :+
    sta 108*BLIT_STRIDE + blitRoll,x
:   jsr e_109oto
    bmi :+
    sta 112*BLIT_STRIDE + blitRoll,x
:   jsr e_113oro
    jsr e_116ooto
    jsr e_120ooro
    jmp e_124otoo

e_60rooo:
    bmi :+
    sta 60*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 61*BLIT_STRIDE + blitRoll,x
    sta 62*BLIT_STRIDE + blitRoll,x
    sta 63*BLIT_STRIDE + blitRoll,x
:   rts

e_t0oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
:   rts

e_t64oroto:
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
    sta 65*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 66*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 67*BLIT_STRIDE + blitRoll,x
:   rts

e_r116otoo:
    asl
    bmi :+
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
:   iny
    lda (pTex),y
    bmi :+
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
:   rts

e_t64oroo:
    iny
    lda (pTex),y
    bmi :+
    sta 64*BLIT_STRIDE + blitRoll,x
    sta 65*BLIT_STRIDE + blitRoll,x
:   asl
    bmi :+
    sta 66*BLIT_STRIDE + blitRoll,x
    sta 67*BLIT_STRIDE + blitRoll,x
:   rts


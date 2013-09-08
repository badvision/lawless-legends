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
e_45orooto:
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    bra e_48to

e_45roooto:
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
    bra e_48to

e_45tooroto:
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    bra e_47roto

e_45torooto:
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    bra e_48to

e_45rotoroto:
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
e_46toroto:
    sta 46*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_47roto:
    sta 47*BLIT_STRIDE + blitRoll,x
    lsr
e_48to:
    sta 48*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 49*BLIT_STRIDE + blitRoll,x
    rts

e_r45tooroto:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    bra e_47roto

e_r45torooto:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    bra e_48to

e_t45orooto:
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    bra e_48to

e_60orooto:
    sta 60*BLIT_STRIDE + blitRoll,x
    sta 61*BLIT_STRIDE + blitRoll,x
    lsr
    sta 62*BLIT_STRIDE + blitRoll,x
    bra e_63to

e_60roooto:
    sta 60*BLIT_STRIDE + blitRoll,x
    lsr
    sta 61*BLIT_STRIDE + blitRoll,x
    sta 62*BLIT_STRIDE + blitRoll,x
    bra e_63to

e_60tooroto:
    sta 60*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 61*BLIT_STRIDE + blitRoll,x
    bra e_62roto

e_r60otoroto:
    lsr
    sta 60*BLIT_STRIDE + blitRoll,x
    bra e_61toroto

e_60rotoroto:
    sta 60*BLIT_STRIDE + blitRoll,x
    lsr
e_61toroto:
    sta 61*BLIT_STRIDE + blitRoll,x
e_t62roto:
    lda (pTex),y
    iny
e_62roto:
    sta 62*BLIT_STRIDE + blitRoll,x
    lsr
e_63to:
    sta 63*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 64*BLIT_STRIDE + blitRoll,x
    rts

e_r60tooroto:
    lsr
    sta 60*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 61*BLIT_STRIDE + blitRoll,x
    bra e_62roto

e_t60orooto:
    lda (pTex),y
    iny
    sta 60*BLIT_STRIDE + blitRoll,x
    sta 61*BLIT_STRIDE + blitRoll,x
    lsr
    sta 62*BLIT_STRIDE + blitRoll,x
    bra e_63to

e_t60rotoroto:
    lda (pTex),y
    iny
    bra e_60rotoroto

e_30orooto:
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    bra e_33to

e_30roooto:
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
    sta 31*BLIT_STRIDE + blitRoll,x
    sta 32*BLIT_STRIDE + blitRoll,x
    bra e_33to

e_30rotoroto:
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
    sta 31*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_32roto

e_30torooto:
    sta 30*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    bra e_33to

e_r30tooroto:
    lsr
    bra e_30tooroto

e_30tooroto:
    sta 30*BLIT_STRIDE + blitRoll,x
e_t31oroto:
    lda (pTex),y
    iny
    sta 31*BLIT_STRIDE + blitRoll,x
e_32roto:
    sta 32*BLIT_STRIDE + blitRoll,x
    lsr
e_33to:
    sta 33*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 34*BLIT_STRIDE + blitRoll,x
    rts

e_r30torooto:
    lsr
    sta 30*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    bra e_33to

e_t30orooto:
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    bra e_33to

e_75orooto:
    sta 75*BLIT_STRIDE + blitRoll,x
    bra e_76rooto

e_75roooto:
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    bra e_77oto

e_75rotoroto:
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_78to

e_75tooroto:
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_78to

e_r75oooto:
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    bra e_77oto

e_75torooto:
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_76rooto:
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
e_77oto:
    sta 77*BLIT_STRIDE + blitRoll,x
e_78to:
    sta 78*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 79*BLIT_STRIDE + blitRoll,x
    rts

e_r75tooroto:
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_78to

e_r75torooto:
    lsr
    bra e_75torooto

e_t75orooto:
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    bra e_76rooto

e_t75rotoroto:
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_78to

e_50orooto:
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    bra e_53to

e_50roooto:
    sta 50*BLIT_STRIDE + blitRoll,x
    lsr
    sta 51*BLIT_STRIDE + blitRoll,x
    sta 52*BLIT_STRIDE + blitRoll,x
    bra e_53to

e_50tooroto:
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    bra e_52roto

e_50rotoroto:
    sta 50*BLIT_STRIDE + blitRoll,x
    lsr
e_51toroto:
    sta 51*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_52roto:
    sta 52*BLIT_STRIDE + blitRoll,x
    lsr
e_53to:
    sta 53*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 54*BLIT_STRIDE + blitRoll,x
    rts

e_r50tooroto:
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    bra e_52roto

e_r50torooto:
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    bra e_53to

e_t50orooto:
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    bra e_53to

e_t50rotoroto:
    lda (pTex),y
    iny
    bra e_50rotoroto

e_80orooto:
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    bra e_83to

e_80roooto:
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    sta 82*BLIT_STRIDE + blitRoll,x
    bra e_83to

e_80torooto:
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    bra e_83to

e_80rotoroto:
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
e_81toroto:
    sta 81*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_82roto:
    sta 82*BLIT_STRIDE + blitRoll,x
    lsr
e_83to:
    sta 83*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 84*BLIT_STRIDE + blitRoll,x
    rts

e_r80tooroto:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    bra e_82roto

e_r80torooto:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    bra e_83to

e_t80orooto:
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    bra e_83to

e_t80rotoroto:
    lda (pTex),y
    iny
    bra e_80rotoroto

e_95roooto:
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
    sta 96*BLIT_STRIDE + blitRoll,x
    sta 97*BLIT_STRIDE + blitRoll,x
    bra e_98to

e_95rotoroto:
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
e_96toroto:
    sta 96*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_97roto:
    sta 97*BLIT_STRIDE + blitRoll,x
    lsr
e_98to:
    sta 98*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 99*BLIT_STRIDE + blitRoll,x
    rts

e_r95torooto:
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    bra e_98to

e_t95orooto:
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    bra e_98to

e_40roooto:
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    sta 42*BLIT_STRIDE + blitRoll,x
    bra e_43to

e_40tooroto:
    sta 40*BLIT_STRIDE + blitRoll,x
e_t41oroto:
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
e_42roto:
    sta 42*BLIT_STRIDE + blitRoll,x
    lsr
e_43to:
    sta 43*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 44*BLIT_STRIDE + blitRoll,x
    rts

e_r40tooroto:
    lsr
    bra e_40tooroto

e_t40orooto:
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    sta 42*BLIT_STRIDE + blitRoll,x
    bra e_43to

e_t40rotoroto:
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_42roto

e_45rootoro:
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_48ro

e_r45otooro:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    bra e_46tooro

e_r45torotoro:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_48ro

e_45rotooro:
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
e_46tooro:
    sta 46*BLIT_STRIDE + blitRoll,x
e_t47oro:
    lda (pTex),y
    iny
e_47oro:
    sta 47*BLIT_STRIDE + blitRoll,x
e_48ro:
    sta 48*BLIT_STRIDE + blitRoll,x
    lsr
    sta 49*BLIT_STRIDE + blitRoll,x
    rts

e_t45orotoro:
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    sta 47*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_48ro

e_t45rotooro:
    lda (pTex),y
    iny
    bra e_45rotooro

e_85roooto:
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    bra e_87oto

e_85torooto:
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_86rooto:
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
e_87oto:
    sta 87*BLIT_STRIDE + blitRoll,x
e_88to:
    sta 88*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 89*BLIT_STRIDE + blitRoll,x
    rts

e_r85tooroto:
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_88to

e_r85torooto:
    lsr
    bra e_85torooto

e_t85orooto:
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    bra e_86rooto

e_t85rotoroto:
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_88to

e_35toooro:
    sta 35*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 36*BLIT_STRIDE + blitRoll,x
    bra e_37oro

e_r35otooro:
    lsr
    sta 35*BLIT_STRIDE + blitRoll,x
    bra e_36tooro

e_r35torotoro:
    lsr
    sta 35*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 36*BLIT_STRIDE + blitRoll,x
    lsr
    sta 37*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_38ro

e_35rotooro:
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
e_36tooro:
    sta 36*BLIT_STRIDE + blitRoll,x
e_t37oro:
    lda (pTex),y
    iny
e_37oro:
    sta 37*BLIT_STRIDE + blitRoll,x
e_38ro:
    sta 38*BLIT_STRIDE + blitRoll,x
    lsr
    sta 39*BLIT_STRIDE + blitRoll,x
    rts

e_t35rootoro:
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    sta 37*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_38ro

e_35rootoo:
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    bra e_37too

e_r35ootoo:
    lsr
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    bra e_37too

e_35torotoo:
    sta 35*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_36rotoo:
    sta 36*BLIT_STRIDE + blitRoll,x
    lsr
e_37too:
    sta 37*BLIT_STRIDE + blitRoll,x
e_t38o:
    lda (pTex),y
    iny
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
    rts

e_t35orotoo:
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    bra e_36rotoo

e_70orooto:
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73to

e_70roooto:
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73to

e_70tooroto:
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    bra e_72roto

e_70rotoroto:
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
e_71toroto:
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_72roto:
    sta 72*BLIT_STRIDE + blitRoll,x
    lsr
e_73to:
    sta 73*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 74*BLIT_STRIDE + blitRoll,x
    rts

e_70torooto:
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73to

e_r70otoroto:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    bra e_71toroto

e_t70rotoroto:
    lda (pTex),y
    iny
    bra e_70rotoroto

e_50toooro:
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    bra e_52oro

e_r50otooro:
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    bra e_51tooro

e_r50torotoro:
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_53ro

e_50rotooro:
    sta 50*BLIT_STRIDE + blitRoll,x
    lsr
e_51tooro:
    sta 51*BLIT_STRIDE + blitRoll,x
e_t52oro:
    lda (pTex),y
    iny
e_52oro:
    sta 52*BLIT_STRIDE + blitRoll,x
e_53ro:
    sta 53*BLIT_STRIDE + blitRoll,x
    lsr
    sta 54*BLIT_STRIDE + blitRoll,x
    rts

e_t50orotoro:
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_53ro

e_t50rootoro:
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    lsr
    sta 51*BLIT_STRIDE + blitRoll,x
    sta 52*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_53ro

e_95toooro:
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    bra e_97oro

e_r95otooro:
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    bra e_96tooro

e_95rotooro:
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
e_96tooro:
    sta 96*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_97oro:
    sta 97*BLIT_STRIDE + blitRoll,x
e_98ro:
    sta 98*BLIT_STRIDE + blitRoll,x
    lsr
    sta 99*BLIT_STRIDE + blitRoll,x
    rts

e_t95orotoro:
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_98ro

e_25roooto:
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
    bra e_28to

e_25tooroto:
    sta 25*BLIT_STRIDE + blitRoll,x
e_t26oroto:
    lda (pTex),y
    iny
    sta 26*BLIT_STRIDE + blitRoll,x
e_27roto:
    sta 27*BLIT_STRIDE + blitRoll,x
    lsr
e_28to:
    sta 28*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 29*BLIT_STRIDE + blitRoll,x
    rts

e_r25tooroto:
    lsr
    bra e_25tooroto

e_r25torooto:
    lsr
    sta 25*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    sta 27*BLIT_STRIDE + blitRoll,x
    bra e_28to

e_t25orooto:
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    sta 27*BLIT_STRIDE + blitRoll,x
    bra e_28to

e_55rootoro:
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    bra e_57toro

e_55rotooro:
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    bra e_58ro

e_r55otooro:
    lsr
    sta 55*BLIT_STRIDE + blitRoll,x
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    bra e_58ro

e_t55orotoro:
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    bra e_56rotoro

e_r55torotoro:
    lsr
    sta 55*BLIT_STRIDE + blitRoll,x
e_t56rotoro:
    lda (pTex),y
    iny
e_56rotoro:
    sta 56*BLIT_STRIDE + blitRoll,x
    lsr
e_57toro:
    sta 57*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_58ro:
    sta 58*BLIT_STRIDE + blitRoll,x
    lsr
    sta 59*BLIT_STRIDE + blitRoll,x
    rts

e_t55rotooro:
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    bra e_58ro

e_30toooro:
    sta 30*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 31*BLIT_STRIDE + blitRoll,x
    bra e_32oro

e_r30otooro:
    lsr
    sta 30*BLIT_STRIDE + blitRoll,x
    bra e_31tooro

e_30rotooro:
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
e_31tooro:
    sta 31*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_32oro:
    sta 32*BLIT_STRIDE + blitRoll,x
e_33ro:
    sta 33*BLIT_STRIDE + blitRoll,x
    lsr
    sta 34*BLIT_STRIDE + blitRoll,x
    rts

e_t30orotoro:
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_33ro

e_85toooro:
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    bra e_87oro

e_r85otooro:
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    bra e_86tooro

e_85rotooro:
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
e_86tooro:
    sta 86*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_87oro:
    sta 87*BLIT_STRIDE + blitRoll,x
e_88ro:
    sta 88*BLIT_STRIDE + blitRoll,x
    lsr
    sta 89*BLIT_STRIDE + blitRoll,x
    rts

e_r85torotoro:
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
    sta 87*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_88ro

e_t85orotoro:
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
    sta 87*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_88ro

e_t85rootoro:
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_88ro

e_25rotooro:
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_26tooro

e_r25otooro:
    lsr
    bra e_25otooro

e_25otooro:
    sta 25*BLIT_STRIDE + blitRoll,x
e_26tooro:
    sta 26*BLIT_STRIDE + blitRoll,x
e_t27oro:
    lda (pTex),y
    iny
e_27oro:
    sta 27*BLIT_STRIDE + blitRoll,x
e_28ro:
    sta 28*BLIT_STRIDE + blitRoll,x
    lsr
    sta 29*BLIT_STRIDE + blitRoll,x
    rts

e_t25orotoro:
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    sta 27*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_28ro

e_t25rootoro:
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_28ro

e_40rootoro:
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    sta 42*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_43ro

e_40toooro:
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
    bra e_42oro

e_r40otooro:
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    bra e_41tooro

e_r40torotoro:
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    sta 42*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_43ro

e_40rotooro:
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
e_41tooro:
    sta 41*BLIT_STRIDE + blitRoll,x
e_t42oro:
    lda (pTex),y
    iny
e_42oro:
    sta 42*BLIT_STRIDE + blitRoll,x
e_43ro:
    sta 43*BLIT_STRIDE + blitRoll,x
    lsr
    sta 44*BLIT_STRIDE + blitRoll,x
    rts

e_t40orotoro:
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    sta 42*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_43ro

e_90roooto:
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    sta 92*BLIT_STRIDE + blitRoll,x
    bra e_93to

e_90tooroto:
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    bra e_92roto

e_90rotoroto:
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_92roto:
    sta 92*BLIT_STRIDE + blitRoll,x
    lsr
e_93to:
    sta 93*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 94*BLIT_STRIDE + blitRoll,x
    rts

e_r90tooroto:
    lsr
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    bra e_92roto

e_t90orooto:
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    sta 92*BLIT_STRIDE + blitRoll,x
    bra e_93to

e_t90rotoroto:
    lda (pTex),y
    iny
    bra e_90rotoroto

e_r40ootoo:
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    bra e_41otoo

e_40rootoo:
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
e_41otoo:
    sta 41*BLIT_STRIDE + blitRoll,x
e_42too:
    sta 42*BLIT_STRIDE + blitRoll,x
e_t43o:
    lda (pTex),y
    iny
    sta 43*BLIT_STRIDE + blitRoll,x
    sta 44*BLIT_STRIDE + blitRoll,x
    rts

e_t40orotoo:
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_42too

e_35tooroo:
    sta 35*BLIT_STRIDE + blitRoll,x
e_t36oroo:
    lda (pTex),y
    iny
e_36oroo:
    sta 36*BLIT_STRIDE + blitRoll,x
e_37roo:
    sta 37*BLIT_STRIDE + blitRoll,x
    lsr
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
    rts

e_t35ooroo:
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    bra e_36oroo

e_80rootoro:
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    sta 82*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_83ro

e_80toooro:
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    bra e_82oro

e_r80otooro:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    bra e_81tooro

e_r80torotoro:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_83ro

e_80rotooro:
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
e_81tooro:
    sta 81*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_82oro:
    sta 82*BLIT_STRIDE + blitRoll,x
e_83ro:
    sta 83*BLIT_STRIDE + blitRoll,x
    lsr
    sta 84*BLIT_STRIDE + blitRoll,x
    rts

e_t80orotoro:
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_83ro

e_t80rotooro:
    lda (pTex),y
    iny
    bra e_80rotooro

e_90rotooro:
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 92*BLIT_STRIDE + blitRoll,x
    bra e_93ro

e_90torotoro:
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
e_92toro:
    sta 92*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_93ro:
    sta 93*BLIT_STRIDE + blitRoll,x
    lsr
    sta 94*BLIT_STRIDE + blitRoll,x
    rts

e_r90otooro:
    lsr
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 92*BLIT_STRIDE + blitRoll,x
    bra e_93ro

e_r90torotoro:
    lsr
    bra e_90torotoro

e_t90rootoro:
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    bra e_92toro

e_55orooto:
    sta 55*BLIT_STRIDE + blitRoll,x
    bra e_56rooto

e_55roooto:
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    bra e_57oto

e_55rotoroto:
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_58to

e_55torooto:
    sta 55*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_56rooto:
    sta 56*BLIT_STRIDE + blitRoll,x
    lsr
e_57oto:
    sta 57*BLIT_STRIDE + blitRoll,x
e_58to:
    sta 58*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 59*BLIT_STRIDE + blitRoll,x
    rts

e_r55otoroto:
    lsr
    sta 55*BLIT_STRIDE + blitRoll,x
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_58to

e_r55tooroto:
    lsr
    sta 55*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 56*BLIT_STRIDE + blitRoll,x
    sta 57*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_58to

e_35rotoroto:
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 37*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_38to

e_35torooto:
    sta 35*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_36rooto

e_35orooto:
    sta 35*BLIT_STRIDE + blitRoll,x
e_36rooto:
    sta 36*BLIT_STRIDE + blitRoll,x
    lsr
e_37oto:
    sta 37*BLIT_STRIDE + blitRoll,x
e_38to:
    sta 38*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 39*BLIT_STRIDE + blitRoll,x
    rts

e_r35tooroto:
    lsr
    sta 35*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 36*BLIT_STRIDE + blitRoll,x
    sta 37*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_38to

e_t35orooto:
    lda (pTex),y
    iny
    bra e_35orooto

e_50ooroo:
    sta 50*BLIT_STRIDE + blitRoll,x
    bra e_51oroo

e_50tooroo:
    sta 50*BLIT_STRIDE + blitRoll,x
e_t51oroo:
    lda (pTex),y
    iny
e_51oroo:
    sta 51*BLIT_STRIDE + blitRoll,x
e_52roo:
    sta 52*BLIT_STRIDE + blitRoll,x
    lsr
    sta 53*BLIT_STRIDE + blitRoll,x
    sta 54*BLIT_STRIDE + blitRoll,x
    rts

e_r50tooroo:
    lsr
    bra e_50tooroo

e_r20ootoo:
    lsr
    sta 20*BLIT_STRIDE + blitRoll,x
    bra e_21otoo

e_20rootoo:
    sta 20*BLIT_STRIDE + blitRoll,x
    lsr
e_21otoo:
    sta 21*BLIT_STRIDE + blitRoll,x
e_22too:
    sta 22*BLIT_STRIDE + blitRoll,x
e_t23o:
    lda (pTex),y
    iny
    sta 23*BLIT_STRIDE + blitRoll,x
    sta 24*BLIT_STRIDE + blitRoll,x
    rts

e_t20orotoo:
    lda (pTex),y
    iny
    sta 20*BLIT_STRIDE + blitRoll,x
    sta 21*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_22too

e_75rotooro:
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    bra e_78ro

e_75toooro:
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
    bra e_78ro

e_r75otooro:
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    bra e_78ro

e_75rootoro:
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
e_77toro:
    sta 77*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_78ro:
    sta 78*BLIT_STRIDE + blitRoll,x
    lsr
    sta 79*BLIT_STRIDE + blitRoll,x
    rts

e_r75torotoro:
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_77toro

e_t75orotoro:
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_77toro

e_t75rootoro:
    lda (pTex),y
    iny
    bra e_75rootoro

e_100tooroto:
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 101*BLIT_STRIDE + blitRoll,x
    sta 102*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_103to

e_100orooto:
    sta 100*BLIT_STRIDE + blitRoll,x
e_101rooto:
    sta 101*BLIT_STRIDE + blitRoll,x
    lsr
e_102oto:
    sta 102*BLIT_STRIDE + blitRoll,x
e_103to:
    sta 103*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 104*BLIT_STRIDE + blitRoll,x
    rts

e_r100tooroto:
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 101*BLIT_STRIDE + blitRoll,x
    sta 102*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_103to

e_t100orooto:
    lda (pTex),y
    iny
    bra e_100orooto

e_105roooto:
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    sta 106*BLIT_STRIDE + blitRoll,x
    bra e_107oto

e_105tooroto:
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_108to

e_105orooto:
    sta 105*BLIT_STRIDE + blitRoll,x
e_106rooto:
    sta 106*BLIT_STRIDE + blitRoll,x
    lsr
e_107oto:
    sta 107*BLIT_STRIDE + blitRoll,x
e_108to:
    sta 108*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 109*BLIT_STRIDE + blitRoll,x
    rts

e_105torooto:
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_106rooto

e_15roooto:
    sta 15*BLIT_STRIDE + blitRoll,x
    lsr
    sta 16*BLIT_STRIDE + blitRoll,x
    bra e_17oto

e_15orooto:
    sta 15*BLIT_STRIDE + blitRoll,x
e_16rooto:
    sta 16*BLIT_STRIDE + blitRoll,x
    lsr
e_17oto:
    sta 17*BLIT_STRIDE + blitRoll,x
e_18to:
    sta 18*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 19*BLIT_STRIDE + blitRoll,x
    rts

e_t15orooto:
    lda (pTex),y
    iny
    bra e_15orooto

e_20roooto:
    sta 20*BLIT_STRIDE + blitRoll,x
    lsr
    sta 21*BLIT_STRIDE + blitRoll,x
    bra e_22oto

e_20torooto:
    sta 20*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_21rooto

e_20orooto:
    sta 20*BLIT_STRIDE + blitRoll,x
e_21rooto:
    sta 21*BLIT_STRIDE + blitRoll,x
    lsr
e_22oto:
    sta 22*BLIT_STRIDE + blitRoll,x
e_23to:
    sta 23*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 24*BLIT_STRIDE + blitRoll,x
    rts

e_t20orooto:
    lda (pTex),y
    iny
    bra e_20orooto

e_20rotooro:
    sta 20*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_21tooro

e_20otooro:
    sta 20*BLIT_STRIDE + blitRoll,x
e_21tooro:
    sta 21*BLIT_STRIDE + blitRoll,x
e_t22oro:
    lda (pTex),y
    iny
e_22oro:
    sta 22*BLIT_STRIDE + blitRoll,x
e_23ro:
    sta 23*BLIT_STRIDE + blitRoll,x
    lsr
    sta 24*BLIT_STRIDE + blitRoll,x
    rts

e_20toooro:
    sta 20*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 21*BLIT_STRIDE + blitRoll,x
    bra e_22oro

e_r20otooro:
    lsr
    bra e_20otooro

e_80rootoo:
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    bra e_82too

e_r80ootoo:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    bra e_82too

e_80torotoo:
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_81rotoo:
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
e_82too:
    sta 82*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 83*BLIT_STRIDE + blitRoll,x
    sta 84*BLIT_STRIDE + blitRoll,x
    rts

e_t80orotoo:
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    bra e_81rotoo

e_r85ootoo:
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    bra e_86otoo

e_85rootoo:
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
e_86otoo:
    sta 86*BLIT_STRIDE + blitRoll,x
e_87too:
    sta 87*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
    rts

e_t85rootoo:
    lda (pTex),y
    iny
    bra e_85rootoo

e_r45ootoo:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    bra e_46otoo

e_45rootoo:
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
e_46otoo:
    sta 46*BLIT_STRIDE + blitRoll,x
e_47too:
    sta 47*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
    rts

e_t45orotoo:
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_47too

e_r75ootoo:
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    bra e_76otoo

e_75rootoo:
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
e_76otoo:
    sta 76*BLIT_STRIDE + blitRoll,x
e_77too:
    sta 77*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
    rts

e_t75orotoo:
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_77too

e_t75rootoo:
    lda (pTex),y
    iny
    bra e_75rootoo

e_105otooro:
    sta 105*BLIT_STRIDE + blitRoll,x
e_106tooro:
    sta 106*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_107oro:
    sta 107*BLIT_STRIDE + blitRoll,x
e_108ro:
    sta 108*BLIT_STRIDE + blitRoll,x
    lsr
    sta 109*BLIT_STRIDE + blitRoll,x
    rts

e_r105otooro:
    lsr
    bra e_105otooro

e_90rootoo:
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_91otoo

e_90ootoo:
    sta 90*BLIT_STRIDE + blitRoll,x
e_91otoo:
    sta 91*BLIT_STRIDE + blitRoll,x
e_92too:
    sta 92*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 93*BLIT_STRIDE + blitRoll,x
    sta 94*BLIT_STRIDE + blitRoll,x
    rts

e_r90ootoo:
    lsr
    bra e_90ootoo

e_t90orotoo:
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_92too

e_r25ootoo:
    lsr
    sta 25*BLIT_STRIDE + blitRoll,x
    bra e_26otoo

e_25rootoo:
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
e_26otoo:
    sta 26*BLIT_STRIDE + blitRoll,x
e_27too:
    sta 27*BLIT_STRIDE + blitRoll,x
e_t28o:
    lda (pTex),y
    iny
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
    rts

e_t25rootoo:
    lda (pTex),y
    iny
    bra e_25rootoo

e_r45otoroo:
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_47roo

e_45tooroo:
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_46oroo:
    sta 46*BLIT_STRIDE + blitRoll,x
e_47roo:
    sta 47*BLIT_STRIDE + blitRoll,x
    lsr
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
    rts

e_r45tooroo:
    lsr
    bra e_45tooroo

e_80ooroo:
    sta 80*BLIT_STRIDE + blitRoll,x
    bra e_81oroo

e_r80otoroo:
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_82roo

e_80tooroo:
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_81oroo:
    sta 81*BLIT_STRIDE + blitRoll,x
e_82roo:
    sta 82*BLIT_STRIDE + blitRoll,x
    lsr
    sta 83*BLIT_STRIDE + blitRoll,x
    sta 84*BLIT_STRIDE + blitRoll,x
    rts

e_r80tooroo:
    lsr
    bra e_80tooroo

e_r95otoroo:
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_97roo

e_95tooroo:
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_96oroo:
    sta 96*BLIT_STRIDE + blitRoll,x
e_97roo:
    sta 97*BLIT_STRIDE + blitRoll,x
    lsr
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
    rts

e_t95ooroo:
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    bra e_96oroo

e_r55ootoo:
    lsr
    bra e_55ootoo

e_t55orotoo:
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    sta 56*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_57too

e_55ootoo:
    sta 55*BLIT_STRIDE + blitRoll,x
e_56otoo:
    sta 56*BLIT_STRIDE + blitRoll,x
e_57too:
    sta 57*BLIT_STRIDE + blitRoll,x
e_t58o:
    lda (pTex),y
    iny
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
    rts

e_t55rootoo:
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_56otoo

e_r100otoroo:
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_102roo

e_100tooroo:
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_101oroo:
    sta 101*BLIT_STRIDE + blitRoll,x
e_102roo:
    sta 102*BLIT_STRIDE + blitRoll,x
    lsr
    sta 103*BLIT_STRIDE + blitRoll,x
    sta 104*BLIT_STRIDE + blitRoll,x
    rts

e_t100ooroo:
    lda (pTex),y
    iny
    sta 100*BLIT_STRIDE + blitRoll,x
    bra e_101oroo

e_110tooroto:
    sta 110*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 111*BLIT_STRIDE + blitRoll,x
    sta 112*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_113to

e_110orooto:
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lsr
e_112oto:
    sta 112*BLIT_STRIDE + blitRoll,x
e_113to:
    sta 113*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 114*BLIT_STRIDE + blitRoll,x
    rts

e_t110orooto:
    lda (pTex),y
    iny
    bra e_110orooto

e_110rotooro:
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_111tooro

e_110otooro:
    sta 110*BLIT_STRIDE + blitRoll,x
e_111tooro:
    sta 111*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_112oro:
    sta 112*BLIT_STRIDE + blitRoll,x
e_113ro:
    sta 113*BLIT_STRIDE + blitRoll,x
    lsr
    sta 114*BLIT_STRIDE + blitRoll,x
    rts

e_r110otooro:
    lsr
    bra e_110otooro

e_r40otoroo:
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_42roo

e_40tooroo:
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_41oroo:
    sta 41*BLIT_STRIDE + blitRoll,x
e_42roo:
    sta 42*BLIT_STRIDE + blitRoll,x
    lsr
    sta 43*BLIT_STRIDE + blitRoll,x
    sta 44*BLIT_STRIDE + blitRoll,x
    rts

e_t40ooroo:
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    bra e_41oroo

e_90tooroo:
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_91oroo:
    sta 91*BLIT_STRIDE + blitRoll,x
e_92roo:
    sta 92*BLIT_STRIDE + blitRoll,x
    lsr
    sta 93*BLIT_STRIDE + blitRoll,x
    sta 94*BLIT_STRIDE + blitRoll,x
    rts

e_t90ooroo:
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    bra e_91oroo

e_50rootoo:
    sta 50*BLIT_STRIDE + blitRoll,x
    lsr
    sta 51*BLIT_STRIDE + blitRoll,x
    bra e_52too

e_r50ootoo:
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    bra e_52too

e_50torotoo:
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_51rotoo:
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
e_52too:
    sta 52*BLIT_STRIDE + blitRoll,x
e_t53o:
    lda (pTex),y
    iny
    sta 53*BLIT_STRIDE + blitRoll,x
    sta 54*BLIT_STRIDE + blitRoll,x
    rts

e_t50orotoo:
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    bra e_51rotoo

e_70toooro:
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73ro

e_r70otooro:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73ro

e_r70torotoro:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_71rotoro:
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
e_72toro:
    sta 72*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_73ro:
    sta 73*BLIT_STRIDE + blitRoll,x
    lsr
    sta 74*BLIT_STRIDE + blitRoll,x
    rts

e_t70rootoro:
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    bra e_72toro

e_t70rotooro:
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 72*BLIT_STRIDE + blitRoll,x
    bra e_73ro

e_100rootoo:
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_101otoo

e_100ootoo:
    sta 100*BLIT_STRIDE + blitRoll,x
e_101otoo:
    sta 101*BLIT_STRIDE + blitRoll,x
e_102too:
    sta 102*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 103*BLIT_STRIDE + blitRoll,x
    sta 104*BLIT_STRIDE + blitRoll,x
    rts

e_r100ootoo:
    lsr
    bra e_100ootoo

e_t100orotoo:
    lda (pTex),y
    iny
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_102too

e_105rootoo:
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_106otoo

e_105ootoo:
    sta 105*BLIT_STRIDE + blitRoll,x
e_106otoo:
    sta 106*BLIT_STRIDE + blitRoll,x
e_107too:
    sta 107*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 108*BLIT_STRIDE + blitRoll,x
    sta 109*BLIT_STRIDE + blitRoll,x
    rts

e_t105orotoo:
    lda (pTex),y
    iny
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_107too

e_15toooro:
    sta 15*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 16*BLIT_STRIDE + blitRoll,x
    bra e_17oro

e_15rotooro:
    sta 15*BLIT_STRIDE + blitRoll,x
    lsr
e_16tooro:
    sta 16*BLIT_STRIDE + blitRoll,x
e_t17oro:
    lda (pTex),y
    iny
e_17oro:
    sta 17*BLIT_STRIDE + blitRoll,x
e_18ro:
    sta 18*BLIT_STRIDE + blitRoll,x
    lsr
    sta 19*BLIT_STRIDE + blitRoll,x
    rts

e_r15otooro:
    lsr
    sta 15*BLIT_STRIDE + blitRoll,x
    bra e_16tooro

e_10roooto:
    sta 10*BLIT_STRIDE + blitRoll,x
    lsr
    sta 11*BLIT_STRIDE + blitRoll,x
    bra e_12oto

e_10orooto:
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    lsr
e_12oto:
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 14*BLIT_STRIDE + blitRoll,x
    rts

e_t10orooto:
    lda (pTex),y
    iny
    bra e_10orooto

e_10otooo:
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_12oo:
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
    sta 14*BLIT_STRIDE + blitRoll,x
    rts

e_115roooto:
    sta 115*BLIT_STRIDE + blitRoll,x
    lsr
    sta 116*BLIT_STRIDE + blitRoll,x
    bra e_117oto

e_115orooto:
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    lsr
e_117oto:
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 119*BLIT_STRIDE + blitRoll,x
    rts

e_r115oooto:
    lsr
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    bra e_117oto

e_t115orooto:
    lda (pTex),y
    iny
    bra e_115orooto

e_15rootoo:
    sta 15*BLIT_STRIDE + blitRoll,x
    lsr
e_16otoo:
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
e_t18o:
    lda (pTex),y
    iny
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
    rts

e_t15rootoo:
    lda (pTex),y
    iny
    bra e_15rootoo

e_25orooo:
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_27oo

e_25otooo:
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_27oo:
    sta 27*BLIT_STRIDE + blitRoll,x
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
    rts

e_45otooo:
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_47oo:
    sta 47*BLIT_STRIDE + blitRoll,x
    sta 48*BLIT_STRIDE + blitRoll,x
    sta 49*BLIT_STRIDE + blitRoll,x
    rts

e_85otooo:
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_87oo:
    sta 87*BLIT_STRIDE + blitRoll,x
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
    rts

e_105tooroo:
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_106oroo:
    sta 106*BLIT_STRIDE + blitRoll,x
e_107roo:
    sta 107*BLIT_STRIDE + blitRoll,x
    lsr
    sta 108*BLIT_STRIDE + blitRoll,x
    sta 109*BLIT_STRIDE + blitRoll,x
    rts

e_r105tooroo:
    lsr
    bra e_105tooroo

e_t105ooroo:
    lda (pTex),y
    iny
    sta 105*BLIT_STRIDE + blitRoll,x
    bra e_106oroo

e_r15otoroo:
    lsr
    sta 15*BLIT_STRIDE + blitRoll,x
    sta 16*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_17roo

e_15tooroo:
    sta 15*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_16oroo:
    sta 16*BLIT_STRIDE + blitRoll,x
e_17roo:
    sta 17*BLIT_STRIDE + blitRoll,x
    lsr
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
    rts

e_t15ooroo:
    lda (pTex),y
    iny
    sta 15*BLIT_STRIDE + blitRoll,x
    bra e_16oroo

e_20tooroo:
    sta 20*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_21oroo:
    sta 21*BLIT_STRIDE + blitRoll,x
e_22roo:
    sta 22*BLIT_STRIDE + blitRoll,x
    lsr
    sta 23*BLIT_STRIDE + blitRoll,x
    sta 24*BLIT_STRIDE + blitRoll,x
    rts

e_5tooroo:
    sta 5*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_6oroo

e_5ooroo:
    sta 5*BLIT_STRIDE + blitRoll,x
e_6oroo:
    sta 6*BLIT_STRIDE + blitRoll,x
    sta 7*BLIT_STRIDE + blitRoll,x
    lsr
    sta 8*BLIT_STRIDE + blitRoll,x
    sta 9*BLIT_STRIDE + blitRoll,x
    rts

e_t5ooroo:
    lda (pTex),y
    iny
    bra e_5ooroo

e_5toooro:
    sta 5*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 6*BLIT_STRIDE + blitRoll,x
    bra e_7oro

e_5otooro:
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
e_t7oro:
    lda (pTex),y
    iny
e_7oro:
    sta 7*BLIT_STRIDE + blitRoll,x
    sta 8*BLIT_STRIDE + blitRoll,x
    lsr
    sta 9*BLIT_STRIDE + blitRoll,x
    rts

e_r5otooro:
    lsr
    bra e_5otooro

e_110orooo:
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lsr
e_112oo:
    sta 112*BLIT_STRIDE + blitRoll,x
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
    rts

e_110tooroo:
    sta 110*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_111oroo

e_110ooroo:
    sta 110*BLIT_STRIDE + blitRoll,x
e_111oroo:
    sta 111*BLIT_STRIDE + blitRoll,x
e_112roo:
    sta 112*BLIT_STRIDE + blitRoll,x
    lsr
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
    rts

e_r110otoroo:
    lsr
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_112roo

e_t110ooroo:
    lda (pTex),y
    iny
    bra e_110ooroo

e_115otooro:
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_117oro:
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
    lsr
    sta 119*BLIT_STRIDE + blitRoll,x
    rts

e_r115otooro:
    lsr
    bra e_115otooro

e_100toooro:
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 101*BLIT_STRIDE + blitRoll,x
    bra e_102oro

e_r100otooro:
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    bra e_101tooro

e_100rotooro:
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
e_101tooro:
    sta 101*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_102oro:
    sta 102*BLIT_STRIDE + blitRoll,x
e_103ro:
    sta 103*BLIT_STRIDE + blitRoll,x
    lsr
    sta 104*BLIT_STRIDE + blitRoll,x
    rts

e_t100orotoro:
    lda (pTex),y
    iny
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
    lsr
    sta 102*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_103ro

e_t100rootoro:
    lda (pTex),y
    iny
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    sta 101*BLIT_STRIDE + blitRoll,x
    sta 102*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_103ro

e_105orooo:
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_107oo

e_105otooo:
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_107oo:
    sta 107*BLIT_STRIDE + blitRoll,x
    sta 108*BLIT_STRIDE + blitRoll,x
    sta 109*BLIT_STRIDE + blitRoll,x
    rts

e_r105otooo:
    lsr
    bra e_105otooo

e_120orooo:
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_122oo

e_120otooo:
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_122oo:
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
    sta 124*BLIT_STRIDE + blitRoll,x
    rts

e_t120orooo:
    lda (pTex),y
    iny
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_122oo

e_15orooo:
    sta 15*BLIT_STRIDE + blitRoll,x
    sta 16*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_17oo

e_15otooo:
    sta 15*BLIT_STRIDE + blitRoll,x
    sta 16*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_17oo:
    sta 17*BLIT_STRIDE + blitRoll,x
    sta 18*BLIT_STRIDE + blitRoll,x
    sta 19*BLIT_STRIDE + blitRoll,x
    rts

e_r15otooo:
    lsr
    bra e_15otooo

e_25tooroo:
    sta 25*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 26*BLIT_STRIDE + blitRoll,x
    bra e_27roo

e_25rotoroo:
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
e_26toroo:
    sta 26*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_27roo:
    sta 27*BLIT_STRIDE + blitRoll,x
    lsr
    sta 28*BLIT_STRIDE + blitRoll,x
    sta 29*BLIT_STRIDE + blitRoll,x
    rts

e_t25ooroo:
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    bra e_27roo

e_r30ootoo:
    lsr
    sta 30*BLIT_STRIDE + blitRoll,x
    bra e_31otoo

e_30rootoo:
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
e_31otoo:
    sta 31*BLIT_STRIDE + blitRoll,x
e_32too:
    sta 32*BLIT_STRIDE + blitRoll,x
e_t33o:
    lda (pTex),y
    iny
    sta 33*BLIT_STRIDE + blitRoll,x
    sta 34*BLIT_STRIDE + blitRoll,x
    rts

e_t30orotoo:
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_32too

e_85tooroo:
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    bra e_87roo

e_85rotoroo:
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
e_86toroo:
    sta 86*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_87roo:
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    sta 88*BLIT_STRIDE + blitRoll,x
    sta 89*BLIT_STRIDE + blitRoll,x
    rts

e_r85otoroo:
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    bra e_86toroo

e_t85ooroo:
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    bra e_87roo

e_95orooo:
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_97oo

e_95otooo:
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_97oo:
    sta 97*BLIT_STRIDE + blitRoll,x
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
    rts

e_r70tooroo:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    bra e_72roo

e_r70otoroo:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
e_71toroo:
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_72roo:
    sta 72*BLIT_STRIDE + blitRoll,x
    lsr
    sta 73*BLIT_STRIDE + blitRoll,x
    sta 74*BLIT_STRIDE + blitRoll,x
    rts

e_t70ooroo:
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
    bra e_72roo

e_30tooroo:
    sta 30*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_31oroo:
    sta 31*BLIT_STRIDE + blitRoll,x
    sta 32*BLIT_STRIDE + blitRoll,x
    lsr
    sta 33*BLIT_STRIDE + blitRoll,x
    sta 34*BLIT_STRIDE + blitRoll,x
    rts

e_t30ooroo:
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    bra e_31oroo

e_80otooo:
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_82oo

e_80orooo:
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
e_82oo:
    sta 82*BLIT_STRIDE + blitRoll,x
    sta 83*BLIT_STRIDE + blitRoll,x
    sta 84*BLIT_STRIDE + blitRoll,x
    rts

e_r10otooro:
    lsr
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_12oro

e_10toooro:
    sta 10*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_11ooro:
    sta 11*BLIT_STRIDE + blitRoll,x
e_12oro:
    sta 12*BLIT_STRIDE + blitRoll,x
    sta 13*BLIT_STRIDE + blitRoll,x
    lsr
    sta 14*BLIT_STRIDE + blitRoll,x
    rts

e_120roooto:
    sta 120*BLIT_STRIDE + blitRoll,x
    lsr
    sta 121*BLIT_STRIDE + blitRoll,x
    bra e_122oto

e_120orooto:
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lsr
e_122oto:
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 124*BLIT_STRIDE + blitRoll,x
    rts

e_t120orooto:
    lda (pTex),y
    iny
    bra e_120orooto

e_5orooto:
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
    lsr
e_7oto:
    sta 7*BLIT_STRIDE + blitRoll,x
    sta 8*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 9*BLIT_STRIDE + blitRoll,x
    rts

e_t5orooto:
    lda (pTex),y
    iny
    bra e_5orooto

e_120toooro:
    sta 120*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 121*BLIT_STRIDE + blitRoll,x
    bra e_122oro

e_120otooro:
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_122oro:
    sta 122*BLIT_STRIDE + blitRoll,x
    sta 123*BLIT_STRIDE + blitRoll,x
    lsr
    sta 124*BLIT_STRIDE + blitRoll,x
    rts

e_r120otooro:
    lsr
    bra e_120otooro

e_t0ooroo:
    lda (pTex),y
    iny
    sta 0*BLIT_STRIDE + blitRoll,x
    bra e_1oroo

e_t1oroo:
    lda (pTex),y
    iny
e_1oroo:
    sta 1*BLIT_STRIDE + blitRoll,x
    sta 2*BLIT_STRIDE + blitRoll,x
    lsr
    sta 3*BLIT_STRIDE + blitRoll,x
    sta 4*BLIT_STRIDE + blitRoll,x
    rts

e_5orooo:
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_7oo

e_5roooo:
    sta 5*BLIT_STRIDE + blitRoll,x
    lsr
e_6ooo:
    sta 6*BLIT_STRIDE + blitRoll,x
e_7oo:
    sta 7*BLIT_STRIDE + blitRoll,x
    sta 8*BLIT_STRIDE + blitRoll,x
    sta 9*BLIT_STRIDE + blitRoll,x
    rts

e_65rotoroto:
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 67*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_68to

e_65orooto:
    sta 65*BLIT_STRIDE + blitRoll,x
    sta 66*BLIT_STRIDE + blitRoll,x
    lsr
    sta 67*BLIT_STRIDE + blitRoll,x
e_68to:
    sta 68*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 69*BLIT_STRIDE + blitRoll,x
    rts

e_t65orooto:
    lda (pTex),y
    iny
    bra e_65orooto

e_70rootoo:
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    bra e_72too

e_r70torotoo:
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_71rotoo:
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
e_72too:
    sta 72*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 73*BLIT_STRIDE + blitRoll,x
    sta 74*BLIT_STRIDE + blitRoll,x
    rts

e_t70orotoo:
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    bra e_71rotoo

e_r110ootoo:
    lsr
    sta 110*BLIT_STRIDE + blitRoll,x
    bra e_111otoo

e_110rootoo:
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
e_111otoo:
    sta 111*BLIT_STRIDE + blitRoll,x
e_112too:
    sta 112*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 113*BLIT_STRIDE + blitRoll,x
    sta 114*BLIT_STRIDE + blitRoll,x
    rts

e_t110orotoo:
    lda (pTex),y
    iny
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_112too

e_30orooo:
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_32oo

e_30otooo:
    sta 30*BLIT_STRIDE + blitRoll,x
    sta 31*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_32oo:
    sta 32*BLIT_STRIDE + blitRoll,x
    sta 33*BLIT_STRIDE + blitRoll,x
    sta 34*BLIT_STRIDE + blitRoll,x
    rts

e_90orooo:
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_92oo

e_90otooo:
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_92oo:
    sta 92*BLIT_STRIDE + blitRoll,x
    sta 93*BLIT_STRIDE + blitRoll,x
    sta 94*BLIT_STRIDE + blitRoll,x
    rts

e_r95ootoo:
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    bra e_96otoo

e_95rootoo:
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
e_96otoo:
    sta 96*BLIT_STRIDE + blitRoll,x
e_97too:
    sta 97*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 98*BLIT_STRIDE + blitRoll,x
    sta 99*BLIT_STRIDE + blitRoll,x
    rts

e_t95orotoo:
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_97too

e_t0orooto:
    lda (pTex),y
    iny
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_2oto

e_t0roooto:
    lda (pTex),y
    iny
    sta 0*BLIT_STRIDE + blitRoll,x
    lsr
e_1ooto:
    sta 1*BLIT_STRIDE + blitRoll,x
e_2oto:
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 4*BLIT_STRIDE + blitRoll,x
    rts

e_65rotoroo:
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_67roo

e_65ooroo:
    sta 65*BLIT_STRIDE + blitRoll,x
    sta 66*BLIT_STRIDE + blitRoll,x
e_67roo:
    sta 67*BLIT_STRIDE + blitRoll,x
    lsr
    sta 68*BLIT_STRIDE + blitRoll,x
    sta 69*BLIT_STRIDE + blitRoll,x
    rts

e_t65ooroo:
    lda (pTex),y
    iny
    bra e_65ooroo

e_20orooo:
    sta 20*BLIT_STRIDE + blitRoll,x
    sta 21*BLIT_STRIDE + blitRoll,x
    lsr
e_22oo:
    sta 22*BLIT_STRIDE + blitRoll,x
    sta 23*BLIT_STRIDE + blitRoll,x
    sta 24*BLIT_STRIDE + blitRoll,x
    rts

e_60roooo:
    sta 60*BLIT_STRIDE + blitRoll,x
    lsr
    sta 61*BLIT_STRIDE + blitRoll,x
    bra e_62oo

e_60orooo:
    sta 60*BLIT_STRIDE + blitRoll,x
    sta 61*BLIT_STRIDE + blitRoll,x
    lsr
e_62oo:
    sta 62*BLIT_STRIDE + blitRoll,x
    sta 63*BLIT_STRIDE + blitRoll,x
    sta 64*BLIT_STRIDE + blitRoll,x
    rts

e_t60orooo:
    lda (pTex),y
    iny
    bra e_60orooo

e_10rootoo:
    sta 10*BLIT_STRIDE + blitRoll,x
    lsr
e_11otoo:
    sta 11*BLIT_STRIDE + blitRoll,x
    sta 12*BLIT_STRIDE + blitRoll,x
e_t13o:
    lda (pTex),y
    iny
    sta 13*BLIT_STRIDE + blitRoll,x
    sta 14*BLIT_STRIDE + blitRoll,x
    rts

e_r5ootoo:
    lsr
    sta 5*BLIT_STRIDE + blitRoll,x
    bra e_6otoo

e_5rootoo:
    sta 5*BLIT_STRIDE + blitRoll,x
    lsr
e_6otoo:
    sta 6*BLIT_STRIDE + blitRoll,x
    sta 7*BLIT_STRIDE + blitRoll,x
e_t8o:
    lda (pTex),y
    iny
    sta 8*BLIT_STRIDE + blitRoll,x
    sta 9*BLIT_STRIDE + blitRoll,x
    rts

e_t0rootoo:
    lda (pTex),y
    iny
    sta 0*BLIT_STRIDE + blitRoll,x
    lsr
e_1otoo:
    sta 1*BLIT_STRIDE + blitRoll,x
    sta 2*BLIT_STRIDE + blitRoll,x
e_t3o:
    lda (pTex),y
    iny
    sta 3*BLIT_STRIDE + blitRoll,x
    sta 4*BLIT_STRIDE + blitRoll,x
    rts

e_tr0ootoo:
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    bra e_1otoo

e_55tooroo:
    sta 55*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_56oroo:
    sta 56*BLIT_STRIDE + blitRoll,x
e_57roo:
    sta 57*BLIT_STRIDE + blitRoll,x
    lsr
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
    rts

e_75tooroo:
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_76oroo:
    sta 76*BLIT_STRIDE + blitRoll,x
e_77roo:
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
    rts

e_t75ooroo:
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    bra e_76oroo

e_tr0otooro:
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
    bra e_t2oro

e_t2oro:
    lda (pTex),y
    iny
e_2oro:
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
    lsr
    sta 4*BLIT_STRIDE + blitRoll,x
    rts

e_tr0toooro:
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 1*BLIT_STRIDE + blitRoll,x
    bra e_2oro

e_65rotooro:
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 67*BLIT_STRIDE + blitRoll,x
    bra e_68ro

e_65rootoro:
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
e_67toro:
    sta 67*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_68ro:
    sta 68*BLIT_STRIDE + blitRoll,x
    lsr
    sta 69*BLIT_STRIDE + blitRoll,x
    rts

e_r65torotoro:
    lsr
    sta 65*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 66*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_67toro

e_10tooroo:
    sta 10*BLIT_STRIDE + blitRoll,x
e_t11oroo:
    lda (pTex),y
    iny
    sta 11*BLIT_STRIDE + blitRoll,x
    sta 12*BLIT_STRIDE + blitRoll,x
    lsr
    sta 13*BLIT_STRIDE + blitRoll,x
    sta 14*BLIT_STRIDE + blitRoll,x
    rts

e_115otooo:
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_117oo

e_115orooo:
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    lsr
e_117oo:
    sta 117*BLIT_STRIDE + blitRoll,x
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
    rts

e_r125oo:
    lsr
    bra e_125oo

e_125oo:
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
    rts

e_t125oo:
    lda (pTex),y
    iny
    bra e_125oo

e_35otooo:
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_37oo

e_35orooo:
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    lsr
e_37oo:
    sta 37*BLIT_STRIDE + blitRoll,x
    sta 38*BLIT_STRIDE + blitRoll,x
    sta 39*BLIT_STRIDE + blitRoll,x
    rts

e_40otooo:
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_42oo

e_40orooo:
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
e_42oo:
    sta 42*BLIT_STRIDE + blitRoll,x
    sta 43*BLIT_STRIDE + blitRoll,x
    sta 44*BLIT_STRIDE + blitRoll,x
    rts

e_50otooo:
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_52oo:
    sta 52*BLIT_STRIDE + blitRoll,x
    sta 53*BLIT_STRIDE + blitRoll,x
    sta 54*BLIT_STRIDE + blitRoll,x
    rts

e_65orooo:
    sta 65*BLIT_STRIDE + blitRoll,x
    sta 66*BLIT_STRIDE + blitRoll,x
    lsr
    sta 67*BLIT_STRIDE + blitRoll,x
    sta 68*BLIT_STRIDE + blitRoll,x
    sta 69*BLIT_STRIDE + blitRoll,x
    rts

e_t65orooo:
    lda (pTex),y
    iny
    bra e_65orooo

e_70otooo:
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_72oo:
    sta 72*BLIT_STRIDE + blitRoll,x
    sta 73*BLIT_STRIDE + blitRoll,x
    sta 74*BLIT_STRIDE + blitRoll,x
    rts

e_t70orooo:
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_72oo

e_75otooo:
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_77oo:
    sta 77*BLIT_STRIDE + blitRoll,x
    sta 78*BLIT_STRIDE + blitRoll,x
    sta 79*BLIT_STRIDE + blitRoll,x
    rts

e_120rootoo:
    sta 120*BLIT_STRIDE + blitRoll,x
    lsr
e_121otoo:
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 123*BLIT_STRIDE + blitRoll,x
    sta 124*BLIT_STRIDE + blitRoll,x
    rts

e_r120ootoo:
    lsr
    sta 120*BLIT_STRIDE + blitRoll,x
    bra e_121otoo

e_115tooroo:
    sta 115*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_116oroo:
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
    lsr
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
    rts

e_t115ooroo:
    lda (pTex),y
    iny
    sta 115*BLIT_STRIDE + blitRoll,x
    bra e_116oroo

e_120ooroo:
    sta 120*BLIT_STRIDE + blitRoll,x
e_121oroo:
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
    lsr
    sta 123*BLIT_STRIDE + blitRoll,x
    sta 124*BLIT_STRIDE + blitRoll,x
    rts

e_125oro:
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
    lsr
    sta 127*BLIT_STRIDE + blitRoll,x
    rts

e_100orooo:
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
    lsr
e_102oo:
    sta 102*BLIT_STRIDE + blitRoll,x
    sta 103*BLIT_STRIDE + blitRoll,x
    sta 104*BLIT_STRIDE + blitRoll,x
    rts

e_100otooo:
    sta 100*BLIT_STRIDE + blitRoll,x
    sta 101*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_102oo

e_55otooo:
    sta 55*BLIT_STRIDE + blitRoll,x
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
e_57oo:
    sta 57*BLIT_STRIDE + blitRoll,x
    sta 58*BLIT_STRIDE + blitRoll,x
    sta 59*BLIT_STRIDE + blitRoll,x
    rts

e_t55orooo:
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    sta 56*BLIT_STRIDE + blitRoll,x
    lsr
    bra e_57oo

e_t0orooo:
    lda (pTex),y
    iny
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
    lsr
e_2oo:
    sta 2*BLIT_STRIDE + blitRoll,x
    sta 3*BLIT_STRIDE + blitRoll,x
    sta 4*BLIT_STRIDE + blitRoll,x
    rts

e_tr0otooo:
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    sta 1*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    bra e_2oo

; Produce 2 rows from 2 rows
expand_2:
    jsr selectMip5
    lda (pTex),y
    iny
    sta 63*BLIT_STRIDE + blitRoll,x
    lsr
    sta 64*BLIT_STRIDE + blitRoll,x
expand_0:
    rts

; Produce 4 rows from 4 rows
expand_4:
    jsr selectMip4
    jsr e_t62roto
    lsr
    sta 65*BLIT_STRIDE + blitRoll,x
    rts

; Produce 6 rows from 4 rows
expand_6:
    jsr selectMip4
    lda (pTex),y
    iny
    sta 61*BLIT_STRIDE + blitRoll,x
    jsr e_62roto
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    rts

; Produce 8 rows from 8 rows
expand_8:
    jsr selectMip3
    jsr e_t60rotoroto
    lsr
    sta 65*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 66*BLIT_STRIDE + blitRoll,x
    lsr
    sta 67*BLIT_STRIDE + blitRoll,x
    rts

; Produce 10 rows from 8 rows
expand_10:
    jsr selectMip3
    lda (pTex),y
    iny
    sta 59*BLIT_STRIDE + blitRoll,x
    jsr e_60rotoroto
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 67*BLIT_STRIDE + blitRoll,x
    lsr
    sta 68*BLIT_STRIDE + blitRoll,x
    rts

; Produce 12 rows from 8 rows
expand_12:
    jsr selectMip3
    jsr e_t58o
    jsr e_r60tooroto
    jmp e_65rotooro

; Produce 14 rows from 8 rows
expand_14:
    jsr selectMip3
    lda (pTex),y
    iny
    sta 57*BLIT_STRIDE + blitRoll,x
    jsr e_58ro
    jsr e_60tooroto
    jsr e_65rootoo
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    rts

; Produce 16 rows from 16 rows
expand_16:
    jsr selectMip2
    jsr e_t56rotoro
    jsr e_t60rotoroto
    jsr e_r65torotoro
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    rts

; Produce 18 rows from 16 rows
expand_18:
    jsr selectMip2
    jsr e_t55orotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    lsr
    sta 72*BLIT_STRIDE + blitRoll,x
    rts

; Produce 20 rows from 16 rows
expand_20:
    jsr selectMip2
    lda (pTex),y
    iny
    sta 54*BLIT_STRIDE + blitRoll,x
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroto
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    sta 71*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 72*BLIT_STRIDE + blitRoll,x
    lsr
    sta 73*BLIT_STRIDE + blitRoll,x
    rts

; Produce 22 rows from 16 rows
expand_22:
    jsr selectMip2
    jsr e_t53o
    jsr e_r55tooroto
    jsr e_r60otoroto
    jsr e_65rotooro
    jmp e_t70rootoro

; Produce 24 rows from 16 rows
expand_24:
    jsr selectMip2
    jsr e_t52oro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rotooro
    jsr e_t70orotoo
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    rts

; Produce 26 rows from 16 rows
expand_26:
    jsr selectMip2
    jsr e_t51oroo
    jsr e_t55rootoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70tooroto
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    rts

; Produce 28 rows from 16 rows
expand_28:
    jsr selectMip2
    jsr e_t50orooto
    jsr e_55rotooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    sta 77*BLIT_STRIDE + blitRoll,x
    rts

; Produce 30 rows from 16 rows
expand_30:
    jsr selectMip2
    lda (pTex),y
    iny
    sta 49*BLIT_STRIDE + blitRoll,x
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    sta 78*BLIT_STRIDE + blitRoll,x
    rts

; Produce 32 rows from 32 rows
expand_32:
    jsr selectMip1
    lda (pTex),y
    iny
    jsr e_48ro
    jsr e_t50rotoroto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_r65torotoro
    jsr e_t70rotoroto
    jmp e_r75torotoro

; Produce 34 rows from 32 rows
expand_34:
    jsr selectMip1
    jsr e_t47oro
    jsr e_t50rotoroto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoro
    jsr e_t75rotoroto
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    rts

; Produce 36 rows from 32 rows
expand_36:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    jsr e_47roto
    jsr e_r50torotoro
    jsr e_t55orotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoo
    jsr e_r75torotoro
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    rts

; Produce 38 rows from 32 rows
expand_38:
    jsr selectMip1
    jsr e_t45orotoro
    jsr e_t50rootoro
    jsr e_t55rotooro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70otoroto
    jsr e_r75tooroto
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    lsr
    sta 82*BLIT_STRIDE + blitRoll,x
    rts

; Produce 40 rows from 32 rows
expand_40:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 44*BLIT_STRIDE + blitRoll,x
    jsr e_45rotoroto
    jsr e_50rotoroto
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroto
    jsr e_70rotoroto
    jsr e_75rotoroto
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 82*BLIT_STRIDE + blitRoll,x
    lsr
    sta 83*BLIT_STRIDE + blitRoll,x
    rts

; Produce 42 rows from 32 rows
expand_42:
    jsr selectMip1
    jsr e_t43o
    jsr e_r45torooto
    jsr e_r50tooroto
    jsr e_r55otoroto
    jsr e_60rotoroto
    jsr e_65rotoroo
    jsr e_t70rotooro
    jsr e_t75rootoro
    jmp e_t80orotoro

; Produce 44 rows from 32 rows
expand_44:
    jsr selectMip1
    jsr e_t42oro
    jsr e_t45orotoro
    jsr e_50torotoo
    jsr e_r55tooroto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70rootoro
    jsr e_t75orotoo
    jsr e_r80torooto
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    rts

; Produce 46 rows from 32 rows
expand_46:
    jsr selectMip1
    jsr e_t41oroto
    jsr e_45rotooro
    jsr e_t50orotoro
    jsr e_55torooto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70orotoo
    jsr e_r75torooto
    jsr e_r80otoroo
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    rts

; Produce 48 rows from 32 rows
expand_48:
    jsr selectMip1
    jsr e_t40orotoo
    jsr e_r45tooroto
    jsr e_50rotooro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rotooro
    jsr e_t70orotoo
    jsr e_r75tooroto
    jsr e_80rotooro
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
    sta 87*BLIT_STRIDE + blitRoll,x
    rts

; Produce 50 rows from 32 rows
expand_50:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 39*BLIT_STRIDE + blitRoll,x
    jsr e_40rootoro
    jsr e_45torooto
    jsr e_r50otooro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70torooto
    jsr e_r75otooro
    jsr e_t80orotoo
    lsr
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    sta 88*BLIT_STRIDE + blitRoll,x
    rts

; Produce 52 rows from 32 rows
expand_52:
    jsr selectMip1
    jsr e_t38o
    jsr e_r40otoroo
    jsr e_t45orotoo
    jsr e_r50tooroo
    jsr e_t55rootoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70tooroto
    jsr e_75rotooro
    jsr e_80torooto
    jmp e_85rotooro

; Produce 54 rows from 32 rows
expand_54:
    jsr selectMip1
    jsr e_t37oro
    jsr e_40tooroto
    jsr e_45rootoro
    jsr e_50tooroto
    jsr e_55rootoro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    jsr e_t75rootoo
    jsr e_r80tooroo
    jsr e_t85rootoo
    lsr
    sta 90*BLIT_STRIDE + blitRoll,x
    rts

; Produce 56 rows from 32 rows
expand_56:
    jsr selectMip1
    jsr e_t36oroo
    jsr e_t40orotoo
    jsr e_r45otooro
    jsr e_t50orooto
    jsr e_55rotooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    jsr e_t75orotoo
    jsr e_r80otooro
    jsr e_t85orooto
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    rts

; Produce 58 rows from 32 rows
expand_58:
    jsr selectMip1
    jsr e_t35orooto
    jsr e_40rootoo
    jsr e_r45tooroo
    jsr e_t50orooto
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_t75orooto
    jsr e_80rootoro
    jsr e_85tooroo
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    sta 92*BLIT_STRIDE + blitRoll,x
    rts

; Produce 60 rows from 32 rows
expand_60:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 34*BLIT_STRIDE + blitRoll,x
    jsr e_35rootoo
    jsr e_r40otooro
    jsr e_45tooroto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroto
    jsr e_80rootoo
    jsr e_r85otooro
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    sta 92*BLIT_STRIDE + blitRoll,x
    lsr
    sta 93*BLIT_STRIDE + blitRoll,x
    rts

; Produce 62 rows from 32 rows
expand_62:
    jsr selectMip1
    jsr e_t33o
    jsr e_r35otooro
    jsr e_40tooroo
    jsr e_t45orooto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroo
    jsr e_t80orooto
    jsr e_85rootoo
    jmp e_r90otooro

; Produce 64 rows from 64 rows
expand_64:
    jsr selectMip0
    lda (pTex),y
    iny
    jsr e_32roto
    jsr e_r35torotoro
    jsr e_t40rotoroto
    jsr e_r45torotoro
    jsr e_t50rotoroto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_r65torotoro
    jsr e_t70rotoroto
    jsr e_r75torotoro
    jsr e_t80rotoroto
    jsr e_r85torotoro
    jsr e_t90rotoroto
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    rts

; Produce 66 rows from 64 rows
expand_66:
    jsr selectMip0
    jsr e_t31oroto
    jsr e_r35torotoro
    jsr e_t40rotoroto
    jsr e_r45torotoro
    jsr e_t50rotoroto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoro
    jsr e_t75rotoroto
    jsr e_r80torotoro
    jsr e_t85rotoroto
    jsr e_r90torotoro
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
    sta 96*BLIT_STRIDE + blitRoll,x
    rts

; Produce 68 rows from 64 rows
expand_68:
    jsr selectMip0
    jsr e_t30orotoro
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 37*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_38to
    jsr e_r40torotoro
    jsr e_t45rotooro
    jsr e_t50rotoroto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoro
    jsr e_t75rotoroto
    jsr e_r80tooroto
    jsr e_r85torotoro
    jsr e_t90rotoroto
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    rts

; Produce 70 rows from 64 rows
expand_70:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 29*BLIT_STRIDE + blitRoll,x
    jsr e_30rotoroto
    jsr e_r35torotoro
    jsr e_t40orotoro
    lda (pTex),y
    iny
    jsr e_45rotoroto
    jsr e_r50torooto
    jsr e_r55torotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoro
    jsr e_t75orotoro
    jsr e_t80rotoroto
    jsr e_r85torooto
    jsr e_r90torotoro
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
    sta 96*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 97*BLIT_STRIDE + blitRoll,x
    lsr
    sta 98*BLIT_STRIDE + blitRoll,x
    rts

; Produce 72 rows from 64 rows
expand_72:
    jsr selectMip0
    jsr e_t28o
    lsr
    sta 30*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 31*BLIT_STRIDE + blitRoll,x
    lsr
    sta 32*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_33ro
    lda (pTex),y
    iny
    jsr e_35rotooro
    jsr e_t40rotoroto
    jsr e_r45tooroto
    jsr e_r50torotoro
    jsr e_t55orotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoo
    jsr e_r75torotoro
    jsr e_t80rotooro
    jsr e_t85rotoroto
    jsr e_r90tooroto
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_98ro
    rts

; Produce 74 rows from 64 rows
expand_74:
    jsr selectMip0
    jsr e_t27oro
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
    sta 31*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_32roto
    jsr e_35rotoroto
    jsr e_r40tooroto
    jsr e_r45torotoro
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_53ro
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    jsr e_57toro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    lsr
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    jsr e_72roto
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_77too
    jsr e_r80torotoro
    jsr e_t85rootoro
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_92roo
    lda (pTex),y
    iny
    jsr e_95rotoroto
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    rts

; Produce 76 rows from 64 rows
expand_76:
    jsr selectMip0
    jsr e_t26oroto
    jsr e_r30torooto
    lsr
    jsr e_35torotoo
    jsr e_r40torotoro
    jsr e_t45orotoro
    jsr e_t50rootoro
    jsr e_t55rotooro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70otoroto
    jsr e_r75tooroto
    lsr
    jsr e_80torotoo
    jsr e_r85torotoro
    jsr e_90torotoro
    jsr e_t95orotoro
    lda (pTex),y
    iny
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    sta 101*BLIT_STRIDE + blitRoll,x
    rts

; Produce 78 rows from 64 rows
expand_78:
    jsr selectMip0
    jsr e_t25orotoro
    jsr e_t30orotoro
    jsr e_t35rootoro
    lda (pTex),y
    iny
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    sta 42*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_43ro
    jsr e_t45rotooro
    lda (pTex),y
    iny
    jsr e_50rotooro
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    lsr
    sta 56*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_57roo
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_70rotoroto
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_78to
    lsr
    sta 80*BLIT_STRIDE + blitRoll,x
    jsr e_81toroto
    jsr e_r85tooroto
    jsr e_r90tooroto
    jsr e_r95torooto
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 101*BLIT_STRIDE + blitRoll,x
    lsr
    sta 102*BLIT_STRIDE + blitRoll,x
    rts

; Produce 80 rows from 64 rows
expand_80:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 24*BLIT_STRIDE + blitRoll,x
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    sta 26*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_27roto
    jsr e_30rotoroto
    jsr e_35rotoroto
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_42roto
    jsr e_45rotoroto
    jsr e_50rotoroto
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroto
    jsr e_70rotoroto
    jsr e_75rotoroto
    jsr e_80rotoroto
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_88to
    jsr e_90rotoroto
    jsr e_95rotoroto
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    sta 101*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 102*BLIT_STRIDE + blitRoll,x
    lsr
    sta 103*BLIT_STRIDE + blitRoll,x
    rts

; Produce 82 rows from 64 rows
expand_82:
    jsr selectMip0
    jsr e_t23o
    jsr e_r25torooto
    jsr e_r30torooto
    jsr e_r35tooroto
    jsr e_r40tooroto
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    jsr e_46toroto
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    jsr e_51toroto
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroo
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_71toroo
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 77*BLIT_STRIDE + blitRoll,x
    jsr e_78ro
    jsr e_t80rotooro
    jsr e_t85rootoro
    jsr e_t90rootoro
    jsr e_t95orotoro
    jmp e_t100orotoro

; Produce 84 rows from 64 rows
expand_84:
    jsr selectMip0
    jsr e_t22oro
    jsr e_t25rootoro
    jsr e_t30orotoro
    jsr e_35torotoo
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_42too
    jsr e_r45torooto
    jsr e_r50tooroto
    jsr e_r55otoroto
    jsr e_60rotoroto
    jsr e_65rotoroo
    jsr e_t70rotooro
    jsr e_t75rootoro
    jsr e_t80orotoro
    jsr e_t85orotoro
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_92too
    jsr e_r95torooto
    jsr e_r100tooroto
    lsr
    sta 105*BLIT_STRIDE + blitRoll,x
    rts

; Produce 86 rows from 64 rows
expand_86:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 21*BLIT_STRIDE + blitRoll,x
    sta 22*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_23to
    jsr e_25rotoroo
    lda (pTex),y
    iny
    jsr e_30rotooro
    jsr e_t35rootoro
    jsr e_t40orotoro
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_47too
    jsr e_r50torooto
    jsr e_r55tooroto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70rootoro
    jsr e_t75orotoro
    jsr e_80torotoo
    jsr e_r85torooto
    jsr e_r90tooroto
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    jsr e_96toroto
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    sta 101*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_102roo
    lda (pTex),y
    iny
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    sta 106*BLIT_STRIDE + blitRoll,x
    rts

; Produce 88 rows from 64 rows
expand_88:
    jsr selectMip0
    jsr e_t20orotoo
    jsr e_r25torooto
    jsr e_r30tooroto
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_37roo
    lda (pTex),y
    iny
    jsr e_40rotooro
    jsr e_t45orotoro
    jsr e_50torotoo
    jsr e_r55tooroto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70rootoro
    jsr e_t75orotoo
    jsr e_r80torooto
    jsr e_r85tooroto
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_92roo
    lda (pTex),y
    iny
    jsr e_95rotooro
    jsr e_t100orotoro
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 106*BLIT_STRIDE + blitRoll,x
    lsr
    sta 107*BLIT_STRIDE + blitRoll,x
    rts

; Produce 90 rows from 64 rows
expand_90:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 19*BLIT_STRIDE + blitRoll,x
    jsr e_20rotooro
    jsr e_t25orotoro
    jsr e_30torooto
    jsr e_r35tooroto
    sta 40*BLIT_STRIDE + blitRoll,x
    lsr
    sta 41*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_42roo
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_48ro
    jsr e_t50orotoo
    lsr
    jsr e_55torooto
    jsr e_r60otoroto
    jsr e_65rotooro
    lda (pTex),y
    iny
    sta 70*BLIT_STRIDE + blitRoll,x
    jsr e_71rotoro
    jsr e_75torooto
    jsr e_r80tooroto
    jsr e_85rotoroo
    jsr e_t90rootoro
    jsr e_t95orotoo
    lsr
    sta 100*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_101rooto
    lsr
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 107*BLIT_STRIDE + blitRoll,x
    lsr
    sta 108*BLIT_STRIDE + blitRoll,x
    rts

; Produce 92 rows from 64 rows
expand_92:
    jsr selectMip0
    jsr e_t18o
    lsr
    sta 20*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 21*BLIT_STRIDE + blitRoll,x
    sta 22*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_23to
    jsr e_25rotooro
    lda (pTex),y
    iny
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
    sta 31*BLIT_STRIDE + blitRoll,x
    sta 32*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_33ro
    jsr e_35torooto
    jsr e_r40tooroto
    jsr e_45rotooro
    jsr e_t50orotoro
    jsr e_55torooto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70orotoo
    jsr e_r75torooto
    jsr e_r80otoroo
    lda (pTex),y
    iny
    jsr e_85rotooro
    jsr e_t90orotoo
    lsr
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    jsr e_97roto
    jsr e_r100otoroo
    lda (pTex),y
    iny
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_108ro
    rts

; Produce 94 rows from 64 rows
expand_94:
    jsr selectMip0
    jsr e_t17oro
    jsr e_t20orotoo
    jsr e_r25tooroto
    jsr e_30rotooro
    jsr e_t35orotoo
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    sta 42*BLIT_STRIDE + blitRoll,x
    jsr e_43to
    jsr e_r45otoroo
    jsr e_t50rootoro
    jsr e_55torooto
    jsr e_r60otoroto
    jsr e_65rotooro
    jsr e_t70orotoo
    jsr e_r75tooroto
    jsr e_80rotooro
    jsr e_t85orotoro
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    sta 92*BLIT_STRIDE + blitRoll,x
    jsr e_93to
    jsr e_r95otoroo
    jsr e_t100rootoro
    jsr e_105torooto
    lsr
    sta 110*BLIT_STRIDE + blitRoll,x
    rts

; Produce 96 rows from 64 rows
expand_96:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_18to
    jsr e_20rotooro
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_27too
    jsr e_r30tooroto
    jsr e_35rotooro
    jsr e_t40orotoo
    jsr e_r45tooroto
    jsr e_50rotooro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rotooro
    jsr e_t70orotoo
    jsr e_r75tooroto
    jsr e_80rotooro
    lda (pTex),y
    iny
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_87too
    jsr e_r90tooroto
    jsr e_95rotooro
    jsr e_t100orotoo
    lsr
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_108to
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
    sta 111*BLIT_STRIDE + blitRoll,x
    rts

; Produce 98 rows from 64 rows
expand_98:
    jsr selectMip0
    jsr e_t15orooto
    lsr
    sta 20*BLIT_STRIDE + blitRoll,x
    sta 21*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_22roo
    jsr e_t25rootoro
    jsr e_30torooto
    lsr
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_37roo
    jsr e_t40orotoo
    jsr e_r45tooroto
    jsr e_50rotooro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70torooto
    lsr
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_77roo
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    sta 82*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_83ro
    jsr e_85torooto
    jsr e_90rotooro
    jsr e_t95orotoo
    jsr e_r100tooroto
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_106tooro
    lda (pTex),y
    iny
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lsr
    sta 112*BLIT_STRIDE + blitRoll,x
    rts

; Produce 100 rows from 64 rows
expand_100:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 14*BLIT_STRIDE + blitRoll,x
    sta 15*BLIT_STRIDE + blitRoll,x
    lsr
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_18ro
    jsr e_20torooto
    jsr e_r25otooro
    jsr e_t30orotoo
    jsr e_r35tooroto
    jsr e_40rootoro
    jsr e_45torooto
    jsr e_r50otooro
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70torooto
    jsr e_r75otooro
    jsr e_t80orotoo
    jsr e_r85tooroto
    sta 90*BLIT_STRIDE + blitRoll,x
    lsr
    sta 91*BLIT_STRIDE + blitRoll,x
    jsr e_92toro
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    jsr e_98to
    jsr e_r100otooro
    jsr e_t105orotoo
    lsr
    sta 110*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 111*BLIT_STRIDE + blitRoll,x
    sta 112*BLIT_STRIDE + blitRoll,x
    lsr
    sta 113*BLIT_STRIDE + blitRoll,x
    rts

; Produce 102 rows from 64 rows
expand_102:
    jsr selectMip0
    jsr e_t13o
    jsr e_r15otoroo
    lda (pTex),y
    iny
    jsr e_20rootoo
    jsr e_r25tooroto
    sta 30*BLIT_STRIDE + blitRoll,x
    lsr
    sta 31*BLIT_STRIDE + blitRoll,x
    sta 32*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_33ro
    jsr e_35torooto
    jsr e_40rotooro
    jsr e_t45orooto
    lsr
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_52roo
    jsr e_t55orotoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70torooto
    jsr e_75rotooro
    jsr e_t80orooto
    jsr e_r85otoroo
    jsr e_t90orotoo
    lsr
    jsr e_95tooroo
    jsr e_t100rootoro
    jsr e_105tooroto
    jmp e_110rotooro

; Produce 104 rows from 64 rows
expand_104:
    jsr selectMip0
    lda (pTex),y
    iny
    jsr e_12oro
    sta 15*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_16rooto
    jsr e_20rotooro
    jsr e_t25orooto
    jsr e_r30otooro
    jsr e_t35orotoo
    jsr e_r40otoroo
    jsr e_t45orotoo
    jsr e_r50tooroo
    jsr e_t55rootoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70tooroto
    jsr e_75rotooro
    jsr e_80torooto
    jsr e_85rotooro
    jsr e_t90orooto
    jsr e_r95otooro
    jsr e_t100orotoo
    lsr
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_107roo
    jsr e_t110orotoo
    lsr
    sta 115*BLIT_STRIDE + blitRoll,x
    rts

; Produce 106 rows from 64 rows
expand_106:
    jsr selectMip0
    jsr e_t11oroo
    jsr e_t15rootoo
    lsr
    jsr e_20tooroo
    jsr e_t25rootoo
    lsr
    jsr e_30tooroo
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    jsr e_37too
    lsr
    jsr e_40tooroo
    lda (pTex),y
    iny
    jsr e_45rootoo
    jsr e_r50tooroo
    jsr e_t55rootoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70tooroto
    jsr e_75rootoro
    sta 80*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 81*BLIT_STRIDE + blitRoll,x
    jsr e_82roto
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_88ro
    jsr e_90tooroto
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
    sta 96*BLIT_STRIDE + blitRoll,x
    sta 97*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_98ro
    jsr e_100tooroto
    sta 105*BLIT_STRIDE + blitRoll,x
    lsr
    sta 106*BLIT_STRIDE + blitRoll,x
    sta 107*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_108ro
    jsr e_110tooroto
    sta 115*BLIT_STRIDE + blitRoll,x
    lsr
    sta 116*BLIT_STRIDE + blitRoll,x
    rts

; Produce 108 rows from 64 rows
expand_108:
    jsr selectMip0
    jsr e_t10orooto
    jsr e_15rotooro
    jsr e_20torooto
    jsr e_25rotooro
    jsr e_30torooto
    jsr e_35rotooro
    jsr e_40tooroto
    jsr e_45rootoro
    jsr e_50tooroto
    jsr e_55rootoro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    jsr e_t75rootoo
    jsr e_r80tooroo
    jsr e_t85rootoo
    lsr
    jsr e_90tooroo
    jsr e_t95orotoo
    jsr e_r100otoroo
    jsr e_t105orotoo
    jsr e_r110otoroo
    lda (pTex),y
    iny
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    lsr
    sta 117*BLIT_STRIDE + blitRoll,x
    rts

; Produce 110 rows from 64 rows
expand_110:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 9*BLIT_STRIDE + blitRoll,x
    jsr e_10rootoo
    lsr
    jsr e_15tooroo
    jsr e_t20orotoo
    lsr
    sta 25*BLIT_STRIDE + blitRoll,x
    jsr e_26toroo
    jsr e_t30orooto
    jsr e_r35otooro
    jsr e_t40orooto
    jsr e_45rotooro
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    sta 52*BLIT_STRIDE + blitRoll,x
    jsr e_53to
    jsr e_55rootoro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    jsr e_t75orotoo
    jsr e_r80otoroo
    jsr e_t85orooto
    jsr e_r90otooro
    jsr e_t95orooto
    jsr e_100rotooro
    jsr e_105torooto
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
    sta 111*BLIT_STRIDE + blitRoll,x
    sta 112*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_113ro
    sta 115*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
    lsr
    sta 118*BLIT_STRIDE + blitRoll,x
    rts

; Produce 112 rows from 64 rows
expand_112:
    jsr selectMip0
    jsr e_t8o
    jsr e_r10otooro
    jsr e_t15orooto
    jsr e_20rotooro
    jsr e_25tooroto
    jsr e_30rootoo
    lsr
    jsr e_35tooroo
    jsr e_t40orotoo
    jsr e_r45otooro
    jsr e_t50orooto
    jsr e_55rotooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70tooroo
    jsr e_t75orotoo
    jsr e_r80otooro
    jsr e_t85orooto
    jsr e_90rotooro
    sta 95*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 96*BLIT_STRIDE + blitRoll,x
    jsr e_97roto
    jsr e_100rootoo
    jsr e_r105tooroo
    jsr e_t110orotoo
    jmp e_r115otooro

; Produce 114 rows from 64 rows
expand_114:
    jsr selectMip0
    jsr e_t7oro
    jsr e_10tooroo
    jsr e_t15rootoo
    jsr e_r20otooro
    jsr e_t25orooto
    jsr e_30rotooro
    jsr e_35tooroo
    lda (pTex),y
    iny
    jsr e_40rootoo
    jsr e_r45otoroo
    jsr e_t50orooto
    jsr e_55rotooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otoroo
    jsr e_t75orooto
    jsr e_80rotooro
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    sta 87*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_88to
    jsr e_90rootoo
    jsr e_r95otoroo
    jsr e_t100orooto
    jsr e_r105otooro
    jsr e_110tooroto
    jsr e_115rootoo
    lsr
    sta 120*BLIT_STRIDE + blitRoll,x
    rts

; Produce 116 rows from 64 rows
expand_116:
    jsr selectMip0
    lda (pTex),y
    iny
    jsr e_6oroo
    jsr e_t10orooto
    jsr e_15rotooro
    jsr e_20tooroo
    jsr e_t25rootoo
    jsr e_r30otooro
    jsr e_t35orooto
    jsr e_40rootoo
    jsr e_r45tooroo
    jsr e_t50orooto
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_t75orooto
    jsr e_80rootoro
    jsr e_85tooroo
    jsr e_t90orotoo
    jsr e_r95otooro
    jsr e_100tooroto
    jsr e_105rootoo
    jsr e_r110otoroo
    jsr e_t115orooto
    sta 120*BLIT_STRIDE + blitRoll,x
    lsr
    sta 121*BLIT_STRIDE + blitRoll,x
    rts

; Produce 118 rows from 64 rows
expand_118:
    jsr selectMip0
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_r15otoroo
    jsr e_t20orooto
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    sta 26*BLIT_STRIDE + blitRoll,x
    sta 27*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_28ro
    jsr e_30tooroo
    jsr e_t35orooto
    jsr e_40rotooro
    jsr e_45tooroo
    jsr e_t50orotoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75torooto
    jsr e_80rootoo
    jsr e_r85otoroo
    jsr e_t90orooto
    jsr e_95rootoo
    lsr
    jsr e_100tooroo
    lda (pTex),y
    iny
    jsr e_105orooto
    jsr e_110rotooro
    jsr e_115tooroo
    lda (pTex),y
    iny
    sta 120*BLIT_STRIDE + blitRoll,x
    sta 121*BLIT_STRIDE + blitRoll,x
    lsr
    sta 122*BLIT_STRIDE + blitRoll,x
    rts

; Produce 120 rows from 64 rows
expand_120:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 4*BLIT_STRIDE + blitRoll,x
    jsr e_5rootoo
    jsr e_r10otooro
    sta 15*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 16*BLIT_STRIDE + blitRoll,x
    sta 17*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_18to
    jsr e_20rootoo
    jsr e_r25otooro
    jsr e_30tooroto
    jsr e_35rootoo
    jsr e_r40otooro
    jsr e_45tooroto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroto
    jsr e_80rootoo
    jsr e_r85otooro
    jsr e_90tooroto
    jsr e_95rootoo
    jsr e_r100otooro
    jsr e_105tooroto
    jsr e_110rootoo
    jsr e_r115otooro
    sta 120*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 121*BLIT_STRIDE + blitRoll,x
    sta 122*BLIT_STRIDE + blitRoll,x
    lsr
    sta 123*BLIT_STRIDE + blitRoll,x
    rts

; Produce 122 rows from 64 rows
expand_122:
    jsr selectMip0
    jsr e_t3o
    jsr e_r5otooro
    jsr e_10tooroo
    jsr e_t15orooto
    sta 20*BLIT_STRIDE + blitRoll,x
    lsr
    sta 21*BLIT_STRIDE + blitRoll,x
    sta 22*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_23ro
    jsr e_25tooroo
    jsr e_t30orooto
    jsr e_35rootoo
    jsr e_r40otooro
    jsr e_t45orooto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroo
    jsr e_t80orooto
    jsr e_r85otooro
    jsr e_90tooroo
    jsr e_t95orooto
    jsr e_100rootoo
    jsr e_r105tooroo
    jsr e_t110orooto
    jsr e_115rootoo
    jmp e_r120otooro

; Produce 124 rows from 64 rows
expand_124:
    jsr selectMip0
    jsr e_t2oro
    jsr e_5tooroo
    jsr e_t10orooto
    jsr e_15rootoo
    jsr e_r20otooro
    jsr e_25tooroo
    jsr e_t30orotoo
    jsr e_r35otooro
    jsr e_40tooroo
    jsr e_t45orooto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroo
    jsr e_t80orooto
    jsr e_85rootoo
    jsr e_r90otooro
    jsr e_t95orooto
    jsr e_100rootoo
    jsr e_r105otooro
    jsr e_110tooroo
    jsr e_t115orooto
    jsr e_120rootoo
    lsr
    sta 125*BLIT_STRIDE + blitRoll,x
    rts

; Produce 126 rows from 64 rows
expand_126:
    jsr selectMip0
    jsr e_t1oroo
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_r15otooro
    jsr e_20tooroo
    jsr e_t25orooto
    jsr e_30rootoo
    jsr e_r35otooro
    jsr e_40tooroo
    jsr e_t45orooto
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroo
    jsr e_t80orooto
    jsr e_85rootoo
    jsr e_r90otooro
    jsr e_95tooroo
    jsr e_t100orooto
    jsr e_105rootoo
    jsr e_r110otooro
    jsr e_115tooroo
    jsr e_t120orooto
    sta 125*BLIT_STRIDE + blitRoll,x
    lsr
    sta 126*BLIT_STRIDE + blitRoll,x
    rts

; Produce 128 rows from 64 rows
expand_128:
    jsr selectMip0
    jsr e_t0orooto
    jsr e_5rootoo
    jsr e_r10otooro
    jsr e_15tooroo
    jsr e_t20orooto
    jsr e_25rootoo
    jsr e_r30otooro
    jsr e_35tooroo
    jsr e_t40orooto
    jsr e_45rootoo
    jsr e_r50otooro
    jsr e_55tooroo
    jsr e_t60orooto
    jsr e_65rootoo
    jsr e_r70otooro
    jsr e_75tooroo
    jsr e_t80orooto
    jsr e_85rootoo
    jsr e_r90otooro
    jsr e_95tooroo
    jsr e_t100orooto
    jsr e_105rootoo
    jsr e_r110otooro
    jsr e_115tooroo
    jsr e_t120orooto
    jmp e_125roo

; Produce 132 rows from 64 rows
expand_132:
    jsr selectMip0
    jsr e_t0rootoo
    jsr e_r5otooro
    jsr e_10tooroo
    jsr e_t15orooto
    jsr e_20rootoo
    jsr e_r25otooro
    jsr e_30toooro
    jsr e_35tooroo
    jsr e_t40orooto
    jsr e_45rootoo
    jsr e_r50otooro
    jsr e_55tooroo
    jsr e_t60orooto
    jsr e_65orooto
    jsr e_70rootoo
    jsr e_r75otooro
    jsr e_80tooroo
    jsr e_t85orooto
    jsr e_90rootoo
    lsr
    jsr e_95otooo
    jsr e_r100otooro
    jsr e_105tooroo
    jsr e_t110orooto
    jsr e_115rootoo
    jsr e_r120otooro
    jmp e_125too

; Produce 136 rows from 64 rows
expand_136:
    jsr selectMip0
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    jsr e_t1oroo
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_15rootoo
    jsr e_r20otooro
    jsr e_25tooroo
    jsr e_t30ooroo
    jsr e_t35orooto
    jsr e_40rootoo
    lsr
    jsr e_45otooo
    jsr e_r50otooro
    jsr e_55tooroo
    jsr e_t60orooto
    jsr e_65orooto
    jsr e_70rootoo
    jsr e_r75otooro
    jsr e_80toooro
    jsr e_85tooroo
    jsr e_t90orooto
    jsr e_95rootoo
    jsr e_100rootoo
    jsr e_r105otooro
    jsr e_110tooroo
    jsr e_t115ooroo
    jsr e_t120orooto
    jmp e_125roo

; Produce 140 rows from 64 rows
expand_140:
    jsr selectMip0
    ldy #1
    jsr e_t0rootoo
    jsr e_r5ootoo
    jsr e_r10otooro
    jsr e_15toooro
    jsr e_20tooroo
    jsr e_t25orooto
    jsr e_30orooto
    jsr e_35rootoo
    jsr e_r40ootoo
    jsr e_r45otooro
    jsr e_50toooro
    jsr e_55tooroo
    jsr e_t60orooto
    jsr e_65orooto
    jsr e_70rootoo
    jsr e_r75ootoo
    jsr e_r80otooro
    jsr e_85toooro
    jsr e_90tooroo
    jsr e_t95orooto
    jsr e_100orooto
    jsr e_105rootoo
    jsr e_r110ootoo
    jsr e_r115otooro
    jsr e_120toooro
    jmp e_125too

; Produce 144 rows from 64 rows
expand_144:
    jsr selectMip0
    ldy #1
    jsr e_tr0otooro
    jsr e_5tooroo
    jsr e_10tooroo
    lda (pTex),y
    iny
    sta 15*BLIT_STRIDE + blitRoll,x
    sta 16*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_17oo
    jsr e_t20orooto
    jsr e_25roooto
    jsr e_30rootoo
    jsr e_r35ootoo
    jsr e_r40otooro
    sta 45*BLIT_STRIDE + blitRoll,x
    jsr e_46tooro
    jsr e_50tooroo
    jsr e_55tooroo
    jsr e_t60orooo
    jsr e_t65orooto
    jsr e_70roooto
    jsr e_75rootoo
    jsr e_r80ootoo
    jsr e_r85otooro
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 92*BLIT_STRIDE + blitRoll,x
    jsr e_93ro
    jsr e_95tooroo
    jsr e_100tooroo
    lda (pTex),y
    iny
    sta 105*BLIT_STRIDE + blitRoll,x
    sta 106*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_107oo
    jsr e_t110orooto
    jsr e_115roooto
    jsr e_120rootoo
    jmp e_r125oo

; Produce 148 rows from 64 rows
expand_148:
    jsr selectMip0
    ldy #2
    jsr e_t0orooto
    jsr e_5orooto
    jsr e_10roooto
    jsr e_15rootoo
    jsr e_20rootoo
    lsr
    jsr e_25otooo
    jsr e_r30otooro
    sta 35*BLIT_STRIDE + blitRoll,x
    jsr e_36tooro
    jsr e_40toooro
    jsr e_45tooroo
    jsr e_50tooroo
    lda (pTex),y
    iny
    sta 55*BLIT_STRIDE + blitRoll,x
    jsr e_56oroo
    jsr e_t60orooto
    jsr e_65orooto
    jsr e_70roooto
    jsr e_75rootoo
    jsr e_80rootoo
    jsr e_r85ootoo
    lsr
    jsr e_90otooo
    jsr e_r95otooro
    jsr e_100toooro
    jsr e_105tooroo
    jsr e_110tooroo
    jsr e_t115ooroo
    jsr e_t120orooo
    lda (pTex),y
    iny
    jsr e_125oro
    rts

; Produce 152 rows from 64 rows
expand_152:
    jsr selectMip0
    ldy #2
    jsr e_tr0ootoo
    jsr e_r5ootoo
    lsr
    jsr e_10otooo
    jsr e_r15otooro
    jsr e_20otooro
    jsr e_25otooro
    jsr e_30toooro
    jsr e_35tooroo
    jsr e_40tooroo
    jsr e_45tooroo
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    jsr e_51oroo
    jsr e_t55orooo
    jsr e_t60orooo
    jsr e_t65orooto
    jsr e_70orooto
    jsr e_75roooto
    jsr e_80roooto
    jsr e_85rootoo
    jsr e_90rootoo
    jsr e_r95ootoo
    jsr e_r100ootoo
    jsr e_r105otooo
    jsr e_r110otooro
    jsr e_115otooro
    jsr e_120otooro
    jmp e_125too

; Produce 156 rows from 64 rows
expand_156:
    jsr selectMip0
    ldy #2
    jsr e_tr0toooro
    jsr e_5tooroo
    jsr e_10tooroo
    jsr e_15tooroo
    jsr e_20tooroo
    jsr e_t25ooroo
    jsr e_t30ooroo
    jsr e_t35ooroo
    jsr e_t40ooroo
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_47oo
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_52oo
    jsr e_t55orooo
    jsr e_t60orooto
    jsr e_65orooto
    jsr e_70orooto
    jsr e_75orooto
    jsr e_80orooto
    jsr e_85roooto
    jsr e_90roooto
    jsr e_95roooto
    jsr e_100rootoo
    jsr e_105rootoo
    jsr e_110rootoo
    jsr e_115rootoo
    jsr e_120rootoo
    jmp e_r125oo

; Produce 160 rows from 64 rows
expand_160:
    jsr selectMip0
    ldy #3
    jsr e_t0orooto
    jsr e_5orooto
    jsr e_10orooto
    jsr e_15orooto
    jsr e_20orooto
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    lsr
    sta 27*BLIT_STRIDE + blitRoll,x
    jsr e_28to
    jsr e_30orooto
    jsr e_35orooto
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    lsr
    sta 42*BLIT_STRIDE + blitRoll,x
    jsr e_43to
    jsr e_45orooto
    jsr e_50orooto
    jsr e_55orooto
    jsr e_60orooto
    jsr e_65orooto
    jsr e_70orooto
    jsr e_75orooto
    jsr e_80orooto
    sta 85*BLIT_STRIDE + blitRoll,x
    jsr e_86rooto
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    lsr
    sta 92*BLIT_STRIDE + blitRoll,x
    jsr e_93to
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    lsr
    sta 97*BLIT_STRIDE + blitRoll,x
    jsr e_98to
    jsr e_100orooto
    jsr e_105orooto
    jsr e_110orooto
    jsr e_115orooto
    jsr e_120orooto
    jmp e_125oro

; Produce 164 rows from 64 rows
expand_164:
    jsr selectMip0
    ldy #3
    jsr e_tr0ootoo
    jsr e_5rootoo
    jsr e_10rootoo
    jsr e_15rootoo
    jsr e_20rootoo
    jsr e_25roooto
    jsr e_30roooto
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    jsr e_37oto
    jsr e_40roooto
    jsr e_45orooto
    jsr e_50orooto
    jsr e_55orooto
    jsr e_60orooto
    jsr e_65orooo
    jsr e_t70orooo
    lda (pTex),y
    iny
    sta 75*BLIT_STRIDE + blitRoll,x
    sta 76*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_77oo
    lda (pTex),y
    iny
    jsr e_80orooo
    jsr e_t85ooroo
    jsr e_t90ooroo
    jsr e_t95ooroo
    jsr e_t100ooroo
    jsr e_t105ooroo
    jsr e_110tooroo
    jsr e_115tooroo
    sta 120*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_121oroo
    jmp e_125too

; Produce 168 rows from 64 rows
expand_168:
    jsr selectMip0
    ldy #3
    jsr e_tr0otooro
    jsr e_5otooro
    jsr e_10otooo
    jsr e_r15otooo
    jsr e_r20ootoo
    jsr e_r25ootoo
    jsr e_r30ootoo
    jsr e_35rootoo
    jsr e_40roooto
    jsr e_45roooto
    jsr e_50roooto
    jsr e_55orooto
    jsr e_60orooo
    jsr e_t65orooo
    jsr e_t70orooo
    jsr e_t75ooroo
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    jsr e_81oroo
    jsr e_85tooroo
    jsr e_90tooroo
    jsr e_95toooro
    jsr e_100toooro
    jsr e_105otooro
    jsr e_110otooro
    jsr e_115otooo
    lsr
    jsr e_120otooo
    jmp e_r125oo

; Produce 172 rows from 64 rows
expand_172:
    jsr selectMip0
    ldy #4
    jsr e_t0ooroo
    jsr e_t5ooroo
    jsr e_10tooroo
    jsr e_15toooro
    jsr e_20toooro
    jsr e_25otooo
    lsr
    jsr e_30otooo
    jsr e_r35ootoo
    jsr e_r40ootoo
    jsr e_45rootoo
    jsr e_50roooto
    jsr e_55roooto
    jsr e_60orooto
    jsr e_65orooo
    jsr e_t70ooroo
    jsr e_t75ooroo
    jsr e_80tooroo
    jsr e_85toooro
    sta 90*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 91*BLIT_STRIDE + blitRoll,x
    sta 92*BLIT_STRIDE + blitRoll,x
    jsr e_93ro
    sta 95*BLIT_STRIDE + blitRoll,x
    jsr e_96tooro
    jsr e_100otooo
    jsr e_r105otooo
    jsr e_r110ootoo
    jsr e_115rootoo
    jsr e_120roooto
    jmp e_125roo

; Produce 176 rows from 64 rows
expand_176:
    jsr selectMip0
    ldy #4
    jsr e_t0roooto
    jsr e_5orooo
    lda (pTex),y
    iny
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_12oo
    jsr e_t15ooroo
    jsr e_20tooroo
    sta 25*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 26*BLIT_STRIDE + blitRoll,x
    jsr e_27oro
    sta 30*BLIT_STRIDE + blitRoll,x
    jsr e_31tooro
    jsr e_35otooo
    jsr e_r40ootoo
    jsr e_r45ootoo
    jsr e_50roooto
    jsr e_55roooto
    jsr e_60orooo
    jsr e_t65orooo
    jsr e_t70ooroo
    jsr e_75tooroo
    jsr e_80toooro
    sta 85*BLIT_STRIDE + blitRoll,x
    jsr e_86tooro
    jsr e_90otooo
    jsr e_r95ootoo
    jsr e_r100ootoo
    jsr e_105roooto
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
    sta 111*BLIT_STRIDE + blitRoll,x
    jsr e_112oto
    jsr e_115orooo
    jsr e_t120orooo
    jmp e_t125oo

; Produce 180 rows from 64 rows
expand_180:
    jsr selectMip0
    ldy #4
    jsr e_tr0ootoo
    jsr e_r5ootoo
    jsr e_10roooto
    jsr e_15orooo
    lda (pTex),y
    iny
    jsr e_20orooo
    jsr e_t25ooroo
    jsr e_30toooro
    jsr e_35toooro
    jsr e_40otooo
    jsr e_r45ootoo
    jsr e_r50ootoo
    jsr e_55roooto
    jsr e_60orooo
    jsr e_t65orooo
    jsr e_t70ooroo
    jsr e_75toooro
    jsr e_80toooro
    jsr e_85otooo
    jsr e_r90ootoo
    jsr e_r95ootoo
    sta 100*BLIT_STRIDE + blitRoll,x
    lsr
    sta 101*BLIT_STRIDE + blitRoll,x
    jsr e_102oto
    jsr e_105orooo
    lda (pTex),y
    iny
    jsr e_110orooo
    jsr e_t115ooroo
    jsr e_120toooro
    jmp e_125too

; Produce 184 rows from 64 rows
expand_184:
    jsr selectMip0
    ldy #4
    jsr e_tr0toooro
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_7oo
    lsr
    sta 10*BLIT_STRIDE + blitRoll,x
    jsr e_11otoo
    jsr e_15rootoo
    jsr e_20roooto
    jsr e_25orooo
    jsr e_t30ooroo
    jsr e_35toooro
    jsr e_40toooro
    jsr e_45otooo
    jsr e_r50ootoo
    jsr e_55roooto
    jsr e_60orooto
    jsr e_65orooo
    jsr e_t70ooroo
    jsr e_75toooro
    jsr e_80otooo
    lsr
    jsr e_85otooo
    jsr e_r90ootoo
    jsr e_95roooto
    jsr e_100orooo
    jsr e_t105ooroo
    jsr e_t110ooroo
    sta 115*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 116*BLIT_STRIDE + blitRoll,x
    jsr e_117oro
    jsr e_120otooo
    jmp e_r125oo

; Produce 188 rows from 64 rows
expand_188:
    jsr selectMip0
    ldy #5
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_r15otooo
    jsr e_r20ootoo
    jsr e_25roooto
    jsr e_30orooo
    jsr e_t35ooroo
    jsr e_40toooro
    jsr e_45otooo
    jsr e_r50ootoo
    jsr e_55roooto
    jsr e_60orooto
    jsr e_65orooo
    jsr e_t70ooroo
    jsr e_75toooro
    jsr e_80otooo
    jsr e_r85ootoo
    jsr e_90roooto
    jsr e_95orooo
    jsr e_t100ooroo
    sta 105*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 106*BLIT_STRIDE + blitRoll,x
    jsr e_107oro
    sta 110*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 111*BLIT_STRIDE + blitRoll,x
    jsr e_112oro
    jsr e_115otooo
    jsr e_r120ootoo
    jmp e_125roo

; Produce 192 rows from 64 rows
expand_192:
    jsr selectMip0
    ldy #5
    jsr e_t0orooo
    jsr e_t5ooroo
    jsr e_10toooro
    jsr e_15otooo
    jsr e_r20ootoo
    jsr e_25roooto
    jsr e_30orooo
    jsr e_t35ooroo
    jsr e_40toooro
    jsr e_45otooo
    jsr e_r50ootoo
    jsr e_55roooto
    jsr e_60orooo
    jsr e_t65ooroo
    jsr e_70toooro
    jsr e_75otooo
    jsr e_r80ootoo
    jsr e_85roooto
    jsr e_90orooo
    jsr e_t95ooroo
    jsr e_100toooro
    jsr e_105otooo
    jsr e_r110ootoo
    jsr e_115roooto
    jsr e_120orooo
    jmp e_t125oo

; Produce 200 rows from 64 rows
expand_200:
    jsr selectMip0
    ldy #5
    jsr e_tr0otooo
    jsr e_r5ootoo
    sta 10*BLIT_STRIDE + blitRoll,x
    lsr
    sta 11*BLIT_STRIDE + blitRoll,x
    jsr e_12oo
    jsr e_t15ooroo
    jsr e_20toooro
    jsr e_25otooo
    jsr e_r30ootoo
    sta 35*BLIT_STRIDE + blitRoll,x
    lsr
    sta 36*BLIT_STRIDE + blitRoll,x
    jsr e_37oo
    jsr e_t40ooroo
    sta 45*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 46*BLIT_STRIDE + blitRoll,x
    jsr e_47oro
    jsr e_50otooo
    jsr e_r55ootoo
    jsr e_60roooo
    jsr e_t65ooroo
    jsr e_70toooro
    jsr e_75otooo
    jsr e_r80ootoo
    sta 85*BLIT_STRIDE + blitRoll,x
    lsr
    sta 86*BLIT_STRIDE + blitRoll,x
    jsr e_87oo
    jsr e_t90ooroo
    jsr e_95toooro
    jsr e_100otooo
    lsr
    jsr e_105ootoo
    sta 110*BLIT_STRIDE + blitRoll,x
    lsr
    sta 111*BLIT_STRIDE + blitRoll,x
    jsr e_112oo
    jsr e_t115ooroo
    jsr e_120toooro
    sta 125*BLIT_STRIDE + blitRoll,x
    sta 126*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 127*BLIT_STRIDE + blitRoll,x
    rts

; Produce 208 rows from 64 rows
expand_208:
    jsr selectMip0
    ldy #6
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_15roooto
    jsr e_20orooo
    lda (pTex),y
    iny
    sta 25*BLIT_STRIDE + blitRoll,x
    sta 26*BLIT_STRIDE + blitRoll,x
    jsr e_27oro
    jsr e_30otooo
    jsr e_r35ootoo
    jsr e_40orooo
    lda (pTex),y
    iny
    sta 45*BLIT_STRIDE + blitRoll,x
    jsr e_46oroo
    sta 50*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 51*BLIT_STRIDE + blitRoll,x
    jsr e_52oo
    jsr e_r55ootoo
    jsr e_60roooto
    jsr e_65ooroo
    jsr e_70toooro
    jsr e_75otooo
    jsr e_80roooto
    sta 85*BLIT_STRIDE + blitRoll,x
    sta 86*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_87oo
    lda (pTex),y
    iny
    sta 90*BLIT_STRIDE + blitRoll,x
    sta 91*BLIT_STRIDE + blitRoll,x
    sta 92*BLIT_STRIDE + blitRoll,x
    jsr e_93ro
    jsr e_95otooo
    jsr e_r100ootoo
    jsr e_105orooo
    jsr e_t110ooroo
    sta 115*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 116*BLIT_STRIDE + blitRoll,x
    jsr e_117oo
    jsr e_r120ootoo
    jmp e_125roo

; Produce 216 rows from 64 rows
expand_216:
    jsr selectMip0
    ldy #6
    lda (pTex),y
    iny
    lsr
    sta 0*BLIT_STRIDE + blitRoll,x
    jsr e_1ooto
    jsr e_5orooo
    jsr e_10toooro
    jsr e_15otooo
    jsr e_20roooto
    jsr e_25orooo
    jsr e_30toooro
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    jsr e_37too
    jsr e_40roooto
    sta 45*BLIT_STRIDE + blitRoll,x
    jsr e_46oroo
    jsr e_50toooro
    jsr e_55ootoo
    jsr e_60roooo
    jsr e_t65ooroo
    sta 70*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 71*BLIT_STRIDE + blitRoll,x
    jsr e_72oo
    jsr e_r75ootoo
    sta 80*BLIT_STRIDE + blitRoll,x
    lsr
    sta 81*BLIT_STRIDE + blitRoll,x
    jsr e_82oo
    jsr e_t85ooroo
    jsr e_90otooo
    jsr e_r95ootoo
    jsr e_100orooo
    jsr e_t105ooroo
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_112oo
    jsr e_r115oooto
    jsr e_120orooo
    jmp e_t125oo

; Produce 224 rows from 64 rows
expand_224:
    jsr selectMip0
    ldy #6
    jsr e_tr0otooo
    lsr
    sta 5*BLIT_STRIDE + blitRoll,x
    sta 6*BLIT_STRIDE + blitRoll,x
    jsr e_7oto
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_12oo
    jsr e_15toooro
    sta 20*BLIT_STRIDE + blitRoll,x
    jsr e_21otoo
    sta 25*BLIT_STRIDE + blitRoll,x
    lsr
    sta 26*BLIT_STRIDE + blitRoll,x
    jsr e_27oo
    jsr e_t30ooroo
    jsr e_35otooo
    lsr
    sta 40*BLIT_STRIDE + blitRoll,x
    sta 41*BLIT_STRIDE + blitRoll,x
    sta 42*BLIT_STRIDE + blitRoll,x
    jsr e_43to
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    lsr
    jsr e_47oo
    jsr e_50toooro
    jsr e_55ootoo
    jsr e_60roooo
    jsr e_t65ooroo
    jsr e_70otooo
    jsr e_r75oooto
    jsr e_80orooo
    jsr e_85toooro
    jsr e_90ootoo
    sta 95*BLIT_STRIDE + blitRoll,x
    lsr
    sta 96*BLIT_STRIDE + blitRoll,x
    jsr e_97oo
    jsr e_t100ooroo
    jsr e_105otooo
    lsr
    sta 110*BLIT_STRIDE + blitRoll,x
    sta 111*BLIT_STRIDE + blitRoll,x
    jsr e_112oto
    jsr e_115orooo
    jsr e_120toooro
    jmp e_125oo

; Produce 232 rows from 64 rows
expand_232:
    jsr selectMip0
    ldy #7
    jsr e_t0ooroo
    sta 5*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    jsr e_6ooo
    lsr
    sta 10*BLIT_STRIDE + blitRoll,x
    sta 11*BLIT_STRIDE + blitRoll,x
    jsr e_12oto
    jsr e_15orooo
    sta 20*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 21*BLIT_STRIDE + blitRoll,x
    jsr e_22oo
    jsr e_r25ootoo
    jsr e_30orooo
    lda (pTex),y
    iny
    sta 35*BLIT_STRIDE + blitRoll,x
    sta 36*BLIT_STRIDE + blitRoll,x
    jsr e_37oro
    sta 40*BLIT_STRIDE + blitRoll,x
    jsr e_41otoo
    sta 45*BLIT_STRIDE + blitRoll,x
    lsr
    sta 46*BLIT_STRIDE + blitRoll,x
    jsr e_47oo
    lda (pTex),y
    iny
    sta 50*BLIT_STRIDE + blitRoll,x
    sta 51*BLIT_STRIDE + blitRoll,x
    jsr e_52oro
    jsr e_55otooo
    jsr e_60roooto
    jsr e_65ooroo
    jsr e_70otooo
    jsr e_r75oooto
    jsr e_80ooroo
    sta 85*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 86*BLIT_STRIDE + blitRoll,x
    jsr e_87oo
    jsr e_r90ootoo
    jsr e_95orooo
    jsr e_100toooro
    jsr e_105ootoo
    jsr e_110orooo
    lda (pTex),y
    iny
    sta 115*BLIT_STRIDE + blitRoll,x
    sta 116*BLIT_STRIDE + blitRoll,x
    jsr e_117oro
    jsr e_120otooo
    jmp e_125roo

; Produce 240 rows from 64 rows
expand_240:
    jsr selectMip0
    ldy #7
    jsr e_t0roooto
    jsr e_5ooroo
    jsr e_10otooo
    jsr e_15roooto
    sta 20*BLIT_STRIDE + blitRoll,x
    jsr e_21oroo
    jsr e_25otooo
    jsr e_30roooto
    sta 35*BLIT_STRIDE + blitRoll,x
    jsr e_36oroo
    jsr e_40otooo
    jsr e_45roooto
    jsr e_50ooroo
    jsr e_55otooo
    jsr e_60roooto
    jsr e_65ooroo
    jsr e_70otooo
    jsr e_75roooto
    jsr e_80ooroo
    jsr e_85otooo
    jsr e_90roooto
    sta 95*BLIT_STRIDE + blitRoll,x
    jsr e_96oroo
    jsr e_100otooo
    jsr e_105roooto
    jsr e_110ooroo
    jsr e_115otooo
    jsr e_120roooto
    jmp e_125oo

; Produce 248 rows from 64 rows
expand_248:
    jsr selectMip0
    ldy #7
    jsr e_tr0otooo
    jsr e_5roooo
    lda (pTex),y
    iny
    sta 10*BLIT_STRIDE + blitRoll,x
    jsr e_11ooro
    sta 15*BLIT_STRIDE + blitRoll,x
    jsr e_16otoo
    jsr e_20orooo
    sta 25*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 26*BLIT_STRIDE + blitRoll,x
    jsr e_27oo
    jsr e_r30ootoo
    jsr e_35orooo
    sta 40*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 41*BLIT_STRIDE + blitRoll,x
    jsr e_42oo
    lsr
    sta 45*BLIT_STRIDE + blitRoll,x
    sta 46*BLIT_STRIDE + blitRoll,x
    sta 47*BLIT_STRIDE + blitRoll,x
    jsr e_48to
    jsr e_50ooroo
    jsr e_55otooo
    jsr e_60roooto
    jsr e_65ooroo
    jsr e_70otooo
    sta 75*BLIT_STRIDE + blitRoll,x
    lsr
    sta 76*BLIT_STRIDE + blitRoll,x
    jsr e_77oo
    lda (pTex),y
    iny
    sta 80*BLIT_STRIDE + blitRoll,x
    sta 81*BLIT_STRIDE + blitRoll,x
    jsr e_82oro
    sta 85*BLIT_STRIDE + blitRoll,x
    jsr e_86otoo
    jsr e_90orooo
    lda (pTex),y
    iny
    sta 95*BLIT_STRIDE + blitRoll,x
    sta 96*BLIT_STRIDE + blitRoll,x
    jsr e_97oro
    jsr e_100ootoo
    jsr e_105orooo
    sta 110*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 111*BLIT_STRIDE + blitRoll,x
    jsr e_112oo
    jsr e_r115oooto
    jsr e_120ooroo
    jmp e_125too

e_125roo:
    sta 125*BLIT_STRIDE + blitRoll,x
    lsr
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
    rts

e_125too:
    sta 125*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 126*BLIT_STRIDE + blitRoll,x
    sta 127*BLIT_STRIDE + blitRoll,x
    rts

e_115rootoo:
    sta 115*BLIT_STRIDE + blitRoll,x
    lsr
    sta 116*BLIT_STRIDE + blitRoll,x
    sta 117*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 118*BLIT_STRIDE + blitRoll,x
    sta 119*BLIT_STRIDE + blitRoll,x
    rts

e_65rootoo:
    sta 65*BLIT_STRIDE + blitRoll,x
    lsr
    sta 66*BLIT_STRIDE + blitRoll,x
    sta 67*BLIT_STRIDE + blitRoll,x
    lda (pTex),y
    iny
    sta 68*BLIT_STRIDE + blitRoll,x
    sta 69*BLIT_STRIDE + blitRoll,x
    rts


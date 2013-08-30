    .org $800
    .pc02
pTex = $A
selectMip0 = $6003
selectMip1 = selectMip0+3
selectMip2 = selectMip1+3
selectMip3 = selectMip2+3
selectMip4 = selectMip3+3
selectMip5 = selectMip4+3
rowBlit = $B000
ROW_STRIDE = 29

expand_vec1:
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

expand_vec2:
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

e_45roooto:
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
    sta 46*ROW_STRIDE + rowBlit,x
    sta 47*ROW_STRIDE + rowBlit,x
    bra e_48to

e_45tooroto:
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    bra e_47roto

e_45torooto:
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    bra e_48to

e_45rotoroto:
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
e_46toroto:
    sta 46*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_47roto:
    sta 47*ROW_STRIDE + rowBlit,x
    lsr
e_48to:
    sta 48*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 49*ROW_STRIDE + rowBlit,x
    rts

e_r45tooroto:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    bra e_47roto

e_r45torooto:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    bra e_48to

e_t45orooto:
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    bra e_48to

e_60roooto:
    sta 60*ROW_STRIDE + rowBlit,x
    lsr
    sta 61*ROW_STRIDE + rowBlit,x
    sta 62*ROW_STRIDE + rowBlit,x
    bra e_63to

e_60tooroto:
    sta 60*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 61*ROW_STRIDE + rowBlit,x
    bra e_62roto

e_r60otoroto:
    lsr
    sta 60*ROW_STRIDE + rowBlit,x
    bra e_61toroto

e_r60tooroto:
    lsr
    sta 60*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 61*ROW_STRIDE + rowBlit,x
    bra e_62roto

e_60rotoroto:
    sta 60*ROW_STRIDE + rowBlit,x
    lsr
e_61toroto:
    sta 61*ROW_STRIDE + rowBlit,x
e_t62roto:
    lda (pTex),y
    iny
e_62roto:
    sta 62*ROW_STRIDE + rowBlit,x
    lsr
e_63to:
    sta 63*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 64*ROW_STRIDE + rowBlit,x
    rts

e_t60orooto:
    lda (pTex),y
    iny
    sta 60*ROW_STRIDE + rowBlit,x
    sta 61*ROW_STRIDE + rowBlit,x
    lsr
    sta 62*ROW_STRIDE + rowBlit,x
    bra e_63to

e_t60rotoroto:
    lda (pTex),y
    iny
    bra e_60rotoroto

e_30roooto:
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
    sta 31*ROW_STRIDE + rowBlit,x
    sta 32*ROW_STRIDE + rowBlit,x
    bra e_33to

e_30rotoroto:
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
    sta 31*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_32roto

e_30torooto:
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    sta 32*ROW_STRIDE + rowBlit,x
    bra e_33to

e_r30tooroto:
    lsr
    bra e_30tooroto

e_30tooroto:
    sta 30*ROW_STRIDE + rowBlit,x
e_t31oroto:
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
e_32roto:
    sta 32*ROW_STRIDE + rowBlit,x
    lsr
e_33to:
    sta 33*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 34*ROW_STRIDE + rowBlit,x
    rts

e_r30torooto:
    lsr
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    sta 32*ROW_STRIDE + rowBlit,x
    bra e_33to

e_t30orooto:
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    sta 32*ROW_STRIDE + rowBlit,x
    bra e_33to

e_45otooro:
    sta 45*ROW_STRIDE + rowBlit,x
    bra e_46tooro

e_45rootoro:
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
    sta 46*ROW_STRIDE + rowBlit,x
    sta 47*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_48ro

e_45toooro:
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    bra e_47oro

e_r45otooro:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    bra e_46tooro

e_r45torotoro:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_48ro

e_45rotooro:
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
e_46tooro:
    sta 46*ROW_STRIDE + rowBlit,x
e_t47oro:
    lda (pTex),y
    iny
e_47oro:
    sta 47*ROW_STRIDE + rowBlit,x
e_48ro:
    sta 48*ROW_STRIDE + rowBlit,x
    lsr
    sta 49*ROW_STRIDE + rowBlit,x
    rts

e_t45orotoro:
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_48ro

e_t45rotooro:
    lda (pTex),y
    iny
    bra e_45rotooro

e_50orooto:
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    bra e_53to

e_50roooto:
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
    sta 51*ROW_STRIDE + rowBlit,x
    sta 52*ROW_STRIDE + rowBlit,x
    bra e_53to

e_50tooroto:
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    bra e_52roto

e_50rotoroto:
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
e_51toroto:
    sta 51*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_52roto:
    sta 52*ROW_STRIDE + rowBlit,x
    lsr
e_53to:
    sta 53*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 54*ROW_STRIDE + rowBlit,x
    rts

e_r50tooroto:
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    bra e_52roto

e_r50torooto:
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    bra e_53to

e_t50orooto:
    lda (pTex),y
    iny
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    bra e_53to

e_t50rotoroto:
    lda (pTex),y
    iny
    bra e_50rotoroto

e_75orooto:
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    bra e_78to

e_75tooroto:
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    bra e_77roto

e_75torooto:
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    bra e_78to

e_75rotoroto:
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
e_76toroto:
    sta 76*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_77roto:
    sta 77*ROW_STRIDE + rowBlit,x
    lsr
e_78to:
    sta 78*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 79*ROW_STRIDE + rowBlit,x
    rts

e_r75tooroto:
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    bra e_77roto

e_r75torooto:
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    bra e_78to

e_t75orooto:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    bra e_78to

e_t75rotoroto:
    lda (pTex),y
    iny
    bra e_75rotoroto

e_80roooto:
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
    sta 82*ROW_STRIDE + rowBlit,x
    bra e_83to

e_80torooto:
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    bra e_83to

e_80rotoroto:
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
e_81toroto:
    sta 81*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_82roto:
    sta 82*ROW_STRIDE + rowBlit,x
    lsr
e_83to:
    sta 83*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 84*ROW_STRIDE + rowBlit,x
    rts

e_r80tooroto:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    bra e_82roto

e_r80torooto:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    bra e_83to

e_t80orooto:
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    bra e_83to

e_t80rotoroto:
    lda (pTex),y
    iny
    bra e_80rotoroto

e_40orooto:
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    sta 42*ROW_STRIDE + rowBlit,x
    bra e_43to

e_40tooroto:
    sta 40*ROW_STRIDE + rowBlit,x
e_t41oroto:
    lda (pTex),y
    iny
    sta 41*ROW_STRIDE + rowBlit,x
e_42roto:
    sta 42*ROW_STRIDE + rowBlit,x
    lsr
e_43to:
    sta 43*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 44*ROW_STRIDE + rowBlit,x
    rts

e_r40tooroto:
    lsr
    bra e_40tooroto

e_t40orooto:
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    sta 42*ROW_STRIDE + rowBlit,x
    bra e_43to

e_t40rotoroto:
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_42roto

e_50toooro:
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    bra e_52oro

e_r50otooro:
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    bra e_51tooro

e_r50torotoro:
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_53ro

e_50rotooro:
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
e_51tooro:
    sta 51*ROW_STRIDE + rowBlit,x
e_t52oro:
    lda (pTex),y
    iny
e_52oro:
    sta 52*ROW_STRIDE + rowBlit,x
e_53ro:
    sta 53*ROW_STRIDE + rowBlit,x
    lsr
    sta 54*ROW_STRIDE + rowBlit,x
    rts

e_t50orotoro:
    lda (pTex),y
    iny
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_53ro

e_t50rootoro:
    lda (pTex),y
    iny
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
    sta 51*ROW_STRIDE + rowBlit,x
    sta 52*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_53ro

e_55rootoro:
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
    sta 56*ROW_STRIDE + rowBlit,x
    sta 57*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_58ro

e_r55otooro:
    lsr
    sta 55*ROW_STRIDE + rowBlit,x
    bra e_56tooro

e_r55torotoro:
    lsr
    sta 55*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
    sta 57*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_58ro

e_55rotooro:
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
e_56tooro:
    sta 56*ROW_STRIDE + rowBlit,x
e_t57oro:
    lda (pTex),y
    iny
e_57oro:
    sta 57*ROW_STRIDE + rowBlit,x
e_58ro:
    sta 58*ROW_STRIDE + rowBlit,x
    lsr
    sta 59*ROW_STRIDE + rowBlit,x
    rts

e_t55orotoro:
    lda (pTex),y
    iny
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
    sta 57*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_58ro

e_t55rotooro:
    lda (pTex),y
    iny
    bra e_55rotooro

e_95rotoroto:
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
e_96toroto:
    sta 96*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_97roto:
    sta 97*ROW_STRIDE + rowBlit,x
    lsr
e_98to:
    sta 98*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 99*ROW_STRIDE + rowBlit,x
    rts

e_r95torooto:
    lsr
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    bra e_98to

e_t95orooto:
    lda (pTex),y
    iny
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    bra e_98to

e_35rootoo:
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
    bra e_37too

e_r35ootoo:
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    bra e_37too

e_35torotoo:
    sta 35*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_36rotoo:
    sta 36*ROW_STRIDE + rowBlit,x
    lsr
e_37too:
    sta 37*ROW_STRIDE + rowBlit,x
e_t38o:
    lda (pTex),y
    iny
    sta 38*ROW_STRIDE + rowBlit,x
    sta 39*ROW_STRIDE + rowBlit,x
    rts

e_t35orotoo:
    lda (pTex),y
    iny
    sta 35*ROW_STRIDE + rowBlit,x
    bra e_36rotoo

e_70roooto:
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    sta 72*ROW_STRIDE + rowBlit,x
    bra e_73to

e_70tooroto:
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    bra e_72roto

e_70torooto:
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    sta 72*ROW_STRIDE + rowBlit,x
    bra e_73to

e_70rotoroto:
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
e_71toroto:
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_72roto:
    sta 72*ROW_STRIDE + rowBlit,x
    lsr
e_73to:
    sta 73*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 74*ROW_STRIDE + rowBlit,x
    rts

e_r70otoroto:
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    bra e_71toroto

e_t70rotoroto:
    lda (pTex),y
    iny
    bra e_70rotoroto

e_25roooto:
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    sta 26*ROW_STRIDE + rowBlit,x
    sta 27*ROW_STRIDE + rowBlit,x
    bra e_28to

e_25tooroto:
    sta 25*ROW_STRIDE + rowBlit,x
e_t26oroto:
    lda (pTex),y
    iny
    sta 26*ROW_STRIDE + rowBlit,x
e_27roto:
    sta 27*ROW_STRIDE + rowBlit,x
    lsr
e_28to:
    sta 28*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 29*ROW_STRIDE + rowBlit,x
    rts

e_r25tooroto:
    lsr
    bra e_25tooroto

e_r25torooto:
    lsr
    sta 25*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    sta 27*ROW_STRIDE + rowBlit,x
    bra e_28to

e_t25orooto:
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    sta 27*ROW_STRIDE + rowBlit,x
    bra e_28to

e_25rotooro:
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    bra e_26tooro

e_r25otooro:
    lsr
    bra e_25otooro

e_25otooro:
    sta 25*ROW_STRIDE + rowBlit,x
e_26tooro:
    sta 26*ROW_STRIDE + rowBlit,x
e_t27oro:
    lda (pTex),y
    iny
e_27oro:
    sta 27*ROW_STRIDE + rowBlit,x
e_28ro:
    sta 28*ROW_STRIDE + rowBlit,x
    lsr
    sta 29*ROW_STRIDE + rowBlit,x
    rts

e_t25orotoro:
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    sta 27*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_28ro

e_t25rootoro:
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    sta 26*ROW_STRIDE + rowBlit,x
    sta 27*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_28ro

e_35roooto:
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
    bra e_37oto

e_35rotoroto:
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 37*ROW_STRIDE + rowBlit,x
    lsr
    bra e_38to

e_35torooto:
    sta 35*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_36rooto

e_35orooto:
    sta 35*ROW_STRIDE + rowBlit,x
e_36rooto:
    sta 36*ROW_STRIDE + rowBlit,x
    lsr
e_37oto:
    sta 37*ROW_STRIDE + rowBlit,x
e_38to:
    sta 38*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 39*ROW_STRIDE + rowBlit,x
    rts

e_r35tooroto:
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 36*ROW_STRIDE + rowBlit,x
    sta 37*ROW_STRIDE + rowBlit,x
    lsr
    bra e_38to

e_t35orooto:
    lda (pTex),y
    iny
    bra e_35orooto

e_85torooto:
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_86rooto:
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
e_87oto:
    sta 87*ROW_STRIDE + rowBlit,x
e_88to:
    sta 88*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 89*ROW_STRIDE + rowBlit,x
    rts

e_r85tooroto:
    lsr
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    bra e_88to

e_r85torooto:
    lsr
    bra e_85torooto

e_t85orooto:
    lda (pTex),y
    iny
    sta 85*ROW_STRIDE + rowBlit,x
    bra e_86rooto

e_t85rotoroto:
    lda (pTex),y
    iny
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    bra e_88to

e_40rootoro:
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
    sta 42*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_43ro

e_40toooro:
    sta 40*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 41*ROW_STRIDE + rowBlit,x
    bra e_42oro

e_r40otooro:
    lsr
    sta 40*ROW_STRIDE + rowBlit,x
    bra e_41tooro

e_r40torotoro:
    lsr
    sta 40*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    sta 42*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_43ro

e_40rotooro:
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
e_41tooro:
    sta 41*ROW_STRIDE + rowBlit,x
e_t42oro:
    lda (pTex),y
    iny
e_42oro:
    sta 42*ROW_STRIDE + rowBlit,x
e_43ro:
    sta 43*ROW_STRIDE + rowBlit,x
    lsr
    sta 44*ROW_STRIDE + rowBlit,x
    rts

e_t40orotoro:
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    sta 42*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_43ro

e_90roooto:
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    sta 92*ROW_STRIDE + rowBlit,x
    bra e_93to

e_90tooroto:
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92roto

e_90rotoroto:
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_92roto:
    sta 92*ROW_STRIDE + rowBlit,x
    lsr
e_93to:
    sta 93*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 94*ROW_STRIDE + rowBlit,x
    rts

e_r90tooroto:
    lsr
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92roto

e_t90orooto:
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    sta 92*ROW_STRIDE + rowBlit,x
    bra e_93to

e_t90rotoroto:
    lda (pTex),y
    iny
    bra e_90rotoroto

e_35toooro:
    sta 35*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 36*ROW_STRIDE + rowBlit,x
    bra e_37oro

e_r35otooro:
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    bra e_36tooro

e_r35torotoro:
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 36*ROW_STRIDE + rowBlit,x
    lsr
    sta 37*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_38ro

e_35rotooro:
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
e_36tooro:
    sta 36*ROW_STRIDE + rowBlit,x
e_t37oro:
    lda (pTex),y
    iny
e_37oro:
    sta 37*ROW_STRIDE + rowBlit,x
e_38ro:
    sta 38*ROW_STRIDE + rowBlit,x
    lsr
    sta 39*ROW_STRIDE + rowBlit,x
    rts

e_t35rootoro:
    lda (pTex),y
    iny
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
    sta 37*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_38ro

e_35tooroo:
    sta 35*ROW_STRIDE + rowBlit,x
e_t36oroo:
    lda (pTex),y
    iny
e_36oroo:
    sta 36*ROW_STRIDE + rowBlit,x
e_37roo:
    sta 37*ROW_STRIDE + rowBlit,x
    lsr
    sta 38*ROW_STRIDE + rowBlit,x
    sta 39*ROW_STRIDE + rowBlit,x
    rts

e_t35ooroo:
    lda (pTex),y
    iny
    sta 35*ROW_STRIDE + rowBlit,x
    bra e_36oroo

e_r30otooro:
    lsr
    sta 30*ROW_STRIDE + rowBlit,x
    bra e_31tooro

e_30rotooro:
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
e_31tooro:
    sta 31*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_32oro:
    sta 32*ROW_STRIDE + rowBlit,x
e_33ro:
    sta 33*ROW_STRIDE + rowBlit,x
    lsr
    sta 34*ROW_STRIDE + rowBlit,x
    rts

e_t30orotoro:
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    sta 32*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_33ro

e_r75otooro:
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    bra e_76tooro

e_r75torotoro:
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_78ro

e_75rotooro:
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
e_76tooro:
    sta 76*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_77oro:
    sta 77*ROW_STRIDE + rowBlit,x
e_78ro:
    sta 78*ROW_STRIDE + rowBlit,x
    lsr
    sta 79*ROW_STRIDE + rowBlit,x
    rts

e_t75oooro:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    bra e_77oro

e_t75orotoro:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_78ro

e_t75rootoro:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
    sta 76*ROW_STRIDE + rowBlit,x
    sta 77*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_78ro

e_80rootoro:
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
    sta 82*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_83ro

e_80toooro:
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    bra e_82oro

e_r80otooro:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    bra e_81tooro

e_r80torotoro:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_83ro

e_80rotooro:
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
e_81tooro:
    sta 81*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_82oro:
    sta 82*ROW_STRIDE + rowBlit,x
e_83ro:
    sta 83*ROW_STRIDE + rowBlit,x
    lsr
    sta 84*ROW_STRIDE + rowBlit,x
    rts

e_t80orotoro:
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_83ro

e_t80rotooro:
    lda (pTex),y
    iny
    bra e_80rotooro

e_20roooto:
    sta 20*ROW_STRIDE + rowBlit,x
    lsr
    sta 21*ROW_STRIDE + rowBlit,x
    bra e_22oto

e_20torooto:
    sta 20*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_21rooto

e_20orooto:
    sta 20*ROW_STRIDE + rowBlit,x
e_21rooto:
    sta 21*ROW_STRIDE + rowBlit,x
    lsr
e_22oto:
    sta 22*ROW_STRIDE + rowBlit,x
e_23to:
    sta 23*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 24*ROW_STRIDE + rowBlit,x
    rts

e_t20orooto:
    lda (pTex),y
    iny
    bra e_20orooto

e_55orooto:
    sta 55*ROW_STRIDE + rowBlit,x
    bra e_56rooto

e_55roooto:
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
    sta 56*ROW_STRIDE + rowBlit,x
    bra e_57oto

e_55rotoroto:
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
    sta 56*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 57*ROW_STRIDE + rowBlit,x
    lsr
    bra e_58to

e_55torooto:
    sta 55*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_56rooto:
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
e_57oto:
    sta 57*ROW_STRIDE + rowBlit,x
e_58to:
    sta 58*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 59*ROW_STRIDE + rowBlit,x
    rts

e_r55otoroto:
    lsr
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 57*ROW_STRIDE + rowBlit,x
    lsr
    bra e_58to

e_r55tooroto:
    lsr
    sta 55*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 56*ROW_STRIDE + rowBlit,x
    sta 57*ROW_STRIDE + rowBlit,x
    lsr
    bra e_58to

e_80rootoo:
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
    bra e_82too

e_80torotoo:
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_81rotoo:
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
e_82too:
    sta 82*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 83*ROW_STRIDE + rowBlit,x
    sta 84*ROW_STRIDE + rowBlit,x
    rts

e_r80ootoo:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    bra e_82too

e_t80orotoo:
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    bra e_81rotoo

e_15rotooro:
    sta 15*ROW_STRIDE + rowBlit,x
    lsr
    bra e_16tooro

e_15otooro:
    sta 15*ROW_STRIDE + rowBlit,x
e_16tooro:
    sta 16*ROW_STRIDE + rowBlit,x
e_t17oro:
    lda (pTex),y
    iny
e_17oro:
    sta 17*ROW_STRIDE + rowBlit,x
e_18ro:
    sta 18*ROW_STRIDE + rowBlit,x
    lsr
    sta 19*ROW_STRIDE + rowBlit,x
    rts

e_r15otooro:
    lsr
    bra e_15otooro

e_t15oooro:
    lda (pTex),y
    iny
    sta 15*ROW_STRIDE + rowBlit,x
    sta 16*ROW_STRIDE + rowBlit,x
    bra e_17oro

e_45rootoo:
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
e_46otoo:
    sta 46*ROW_STRIDE + rowBlit,x
e_47too:
    sta 47*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 48*ROW_STRIDE + rowBlit,x
    sta 49*ROW_STRIDE + rowBlit,x
    rts

e_r45ootoo:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    bra e_46otoo

e_t45orotoo:
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    bra e_47too

e_r75ootoo:
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    bra e_76otoo

e_75rootoo:
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
e_76otoo:
    sta 76*ROW_STRIDE + rowBlit,x
e_77too:
    sta 77*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 78*ROW_STRIDE + rowBlit,x
    sta 79*ROW_STRIDE + rowBlit,x
    rts

e_t75orotoo:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    bra e_77too

e_t75rootoo:
    lda (pTex),y
    iny
    bra e_75rootoo

e_r70otooro:
    lsr
    bra e_70otooro

e_r70torotoro:
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    sta 72*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_73ro

e_70otooro:
    sta 70*ROW_STRIDE + rowBlit,x
e_71tooro:
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_72oro:
    sta 72*ROW_STRIDE + rowBlit,x
e_73ro:
    sta 73*ROW_STRIDE + rowBlit,x
    lsr
    sta 74*ROW_STRIDE + rowBlit,x
    rts

e_t70oooro:
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    bra e_72oro

e_t70rootoro:
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    sta 72*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_73ro

e_t70rotooro:
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    bra e_71tooro

e_20rootoo:
    sta 20*ROW_STRIDE + rowBlit,x
    lsr
e_21otoo:
    sta 21*ROW_STRIDE + rowBlit,x
e_22too:
    sta 22*ROW_STRIDE + rowBlit,x
e_t23o:
    lda (pTex),y
    iny
    sta 23*ROW_STRIDE + rowBlit,x
    sta 24*ROW_STRIDE + rowBlit,x
    rts

e_t20orotoo:
    lda (pTex),y
    iny
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    lsr
    bra e_22too

e_85rotooro:
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 87*ROW_STRIDE + rowBlit,x
    bra e_88ro

e_r85otooro:
    lsr
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 87*ROW_STRIDE + rowBlit,x
    bra e_88ro

e_85rootoro:
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
e_87toro:
    sta 87*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_88ro:
    sta 88*ROW_STRIDE + rowBlit,x
    lsr
    sta 89*ROW_STRIDE + rowBlit,x
    rts

e_r85torotoro:
    lsr
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
    bra e_87toro

e_t85orotoro:
    lda (pTex),y
    iny
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
    bra e_87toro

e_t85rootoro:
    lda (pTex),y
    iny
    bra e_85rootoro

e_90rotooro:
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 92*ROW_STRIDE + rowBlit,x
    bra e_93ro

e_r90otooro:
    lsr
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 92*ROW_STRIDE + rowBlit,x
    bra e_93ro

e_90torotoro:
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
e_92toro:
    sta 92*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_93ro:
    sta 93*ROW_STRIDE + rowBlit,x
    lsr
    sta 94*ROW_STRIDE + rowBlit,x
    rts

e_r90torotoro:
    lsr
    bra e_90torotoro

e_t90rootoro:
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92toro

e_50rootoo:
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
    bra e_51otoo

e_50torotoo:
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    bra e_52too

e_50ootoo:
    sta 50*ROW_STRIDE + rowBlit,x
e_51otoo:
    sta 51*ROW_STRIDE + rowBlit,x
e_52too:
    sta 52*ROW_STRIDE + rowBlit,x
e_t53o:
    lda (pTex),y
    iny
    sta 53*ROW_STRIDE + rowBlit,x
    sta 54*ROW_STRIDE + rowBlit,x
    rts

e_r50ootoo:
    lsr
    bra e_50ootoo

e_t50orotoo:
    lda (pTex),y
    iny
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    bra e_52too

e_100roooto:
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
    sta 101*ROW_STRIDE + rowBlit,x
    bra e_102oto

e_100tooroto:
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 101*ROW_STRIDE + rowBlit,x
    sta 102*ROW_STRIDE + rowBlit,x
    lsr
    bra e_103to

e_100orooto:
    sta 100*ROW_STRIDE + rowBlit,x
e_101rooto:
    sta 101*ROW_STRIDE + rowBlit,x
    lsr
e_102oto:
    sta 102*ROW_STRIDE + rowBlit,x
e_103to:
    sta 103*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 104*ROW_STRIDE + rowBlit,x
    rts

e_r100tooroto:
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 101*ROW_STRIDE + rowBlit,x
    sta 102*ROW_STRIDE + rowBlit,x
    lsr
    bra e_103to

e_t100orooto:
    lda (pTex),y
    iny
    bra e_100orooto

e_15orooto:
    sta 15*ROW_STRIDE + rowBlit,x
e_16rooto:
    sta 16*ROW_STRIDE + rowBlit,x
    lsr
e_17oto:
    sta 17*ROW_STRIDE + rowBlit,x
e_18to:
    sta 18*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 19*ROW_STRIDE + rowBlit,x
    rts

e_t15orooto:
    lda (pTex),y
    iny
    bra e_15orooto

e_40ooroo:
    sta 40*ROW_STRIDE + rowBlit,x
    bra e_41oroo

e_40tooroo:
    sta 40*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_41oroo:
    sta 41*ROW_STRIDE + rowBlit,x
e_42roo:
    sta 42*ROW_STRIDE + rowBlit,x
    lsr
    sta 43*ROW_STRIDE + rowBlit,x
    sta 44*ROW_STRIDE + rowBlit,x
    rts

e_r40otoroo:
    lsr
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_42roo

e_t40ooroo:
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    bra e_41oroo

e_95toooro:
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    bra e_97oro

e_r95otooro:
    lsr
    sta 95*ROW_STRIDE + rowBlit,x
    bra e_96tooro

e_95rotooro:
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
e_96tooro:
    sta 96*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_97oro:
    sta 97*ROW_STRIDE + rowBlit,x
e_98ro:
    sta 98*ROW_STRIDE + rowBlit,x
    lsr
    sta 99*ROW_STRIDE + rowBlit,x
    rts

e_t95orotoro:
    lda (pTex),y
    iny
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_98ro

e_105torooto:
    sta 105*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    sta 107*ROW_STRIDE + rowBlit,x
    bra e_108to

e_105tooroto:
    sta 105*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 106*ROW_STRIDE + rowBlit,x
    sta 107*ROW_STRIDE + rowBlit,x
    lsr
e_108to:
    sta 108*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 109*ROW_STRIDE + rowBlit,x
    rts

e_r15otoroo:
    lsr
    sta 15*ROW_STRIDE + rowBlit,x
    sta 16*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_17roo

e_15tooroo:
    sta 15*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_16oroo:
    sta 16*ROW_STRIDE + rowBlit,x
e_17roo:
    sta 17*ROW_STRIDE + rowBlit,x
    lsr
    sta 18*ROW_STRIDE + rowBlit,x
    sta 19*ROW_STRIDE + rowBlit,x
    rts

e_t15ooroo:
    lda (pTex),y
    iny
    sta 15*ROW_STRIDE + rowBlit,x
    bra e_16oroo

e_r25ootoo:
    lsr
    sta 25*ROW_STRIDE + rowBlit,x
    bra e_26otoo

e_25rootoo:
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
e_26otoo:
    sta 26*ROW_STRIDE + rowBlit,x
e_27too:
    sta 27*ROW_STRIDE + rowBlit,x
e_t28o:
    lda (pTex),y
    iny
    sta 28*ROW_STRIDE + rowBlit,x
    sta 29*ROW_STRIDE + rowBlit,x
    rts

e_t25rootoo:
    lda (pTex),y
    iny
    bra e_25rootoo

e_r45otoroo:
    lsr
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_47roo

e_45tooroo:
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_46oroo:
    sta 46*ROW_STRIDE + rowBlit,x
e_47roo:
    sta 47*ROW_STRIDE + rowBlit,x
    lsr
    sta 48*ROW_STRIDE + rowBlit,x
    sta 49*ROW_STRIDE + rowBlit,x
    rts

e_r45tooroo:
    lsr
    bra e_45tooroo

e_t45ooroo:
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    bra e_46oroo

e_r55ootoo:
    lsr
    sta 55*ROW_STRIDE + rowBlit,x
    bra e_56otoo

e_t55orotoo:
    lda (pTex),y
    iny
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
    bra e_57too

e_55rootoo:
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
e_56otoo:
    sta 56*ROW_STRIDE + rowBlit,x
e_57too:
    sta 57*ROW_STRIDE + rowBlit,x
e_t58o:
    lda (pTex),y
    iny
    sta 58*ROW_STRIDE + rowBlit,x
    sta 59*ROW_STRIDE + rowBlit,x
    rts

e_t55rootoo:
    lda (pTex),y
    iny
    bra e_55rootoo

e_r100otoroo:
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_102roo

e_100tooroo:
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_101oroo:
    sta 101*ROW_STRIDE + rowBlit,x
e_102roo:
    sta 102*ROW_STRIDE + rowBlit,x
    lsr
    sta 103*ROW_STRIDE + rowBlit,x
    sta 104*ROW_STRIDE + rowBlit,x
    rts

e_t100ooroo:
    lda (pTex),y
    iny
    sta 100*ROW_STRIDE + rowBlit,x
    bra e_101oroo

e_t40orotoo:
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    bra e_42too

e_40rootoo:
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
e_42too:
    sta 42*ROW_STRIDE + rowBlit,x
e_t43o:
    lda (pTex),y
    iny
    sta 43*ROW_STRIDE + rowBlit,x
    sta 44*ROW_STRIDE + rowBlit,x
    rts

e_r50tooroo:
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_51oroo

e_50ooroo:
    sta 50*ROW_STRIDE + rowBlit,x
e_51oroo:
    sta 51*ROW_STRIDE + rowBlit,x
e_52roo:
    sta 52*ROW_STRIDE + rowBlit,x
    lsr
    sta 53*ROW_STRIDE + rowBlit,x
    sta 54*ROW_STRIDE + rowBlit,x
    rts

e_t50ooroo:
    lda (pTex),y
    iny
    bra e_50ooroo

e_r70tooroo:
    lsr
    bra e_70tooroo

e_70tooroo:
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_71oroo:
    sta 71*ROW_STRIDE + rowBlit,x
e_72roo:
    sta 72*ROW_STRIDE + rowBlit,x
    lsr
    sta 73*ROW_STRIDE + rowBlit,x
    sta 74*ROW_STRIDE + rowBlit,x
    rts

e_t70ooroo:
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    bra e_71oroo

e_90otooo:
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_92oo:
    sta 92*ROW_STRIDE + rowBlit,x
    sta 93*ROW_STRIDE + rowBlit,x
    sta 94*ROW_STRIDE + rowBlit,x
    rts

e_90toooo:
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92oo

e_r90otooo:
    lsr
    bra e_90otooo

e_90tooroo:
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_91oroo:
    sta 91*ROW_STRIDE + rowBlit,x
e_92roo:
    sta 92*ROW_STRIDE + rowBlit,x
    lsr
    sta 93*ROW_STRIDE + rowBlit,x
    sta 94*ROW_STRIDE + rowBlit,x
    rts

e_t90ooroo:
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    bra e_91oroo

e_25tooroo:
    sta 25*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_26oroo

e_25ooroo:
    sta 25*ROW_STRIDE + rowBlit,x
e_26oroo:
    sta 26*ROW_STRIDE + rowBlit,x
e_27roo:
    sta 27*ROW_STRIDE + rowBlit,x
    lsr
    sta 28*ROW_STRIDE + rowBlit,x
    sta 29*ROW_STRIDE + rowBlit,x
    rts

e_85tooroo:
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_86oroo

e_85ooroo:
    sta 85*ROW_STRIDE + rowBlit,x
e_86oroo:
    sta 86*ROW_STRIDE + rowBlit,x
e_87roo:
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    sta 88*ROW_STRIDE + rowBlit,x
    sta 89*ROW_STRIDE + rowBlit,x
    rts

e_r85otoroo:
    lsr
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_87roo

e_t85ooroo:
    lda (pTex),y
    iny
    bra e_85ooroo

e_105rotooro:
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
e_106tooro:
    sta 106*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_107oro:
    sta 107*ROW_STRIDE + rowBlit,x
e_108ro:
    sta 108*ROW_STRIDE + rowBlit,x
    lsr
    sta 109*ROW_STRIDE + rowBlit,x
    rts

e_r105otooro:
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    bra e_106tooro

e_t105oooro:
    lda (pTex),y
    iny
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    bra e_107oro

e_65rootoro:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    sta 67*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_68ro

e_65toooro:
    sta 65*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 66*ROW_STRIDE + rowBlit,x
    bra e_67oro

e_65rotooro:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
e_66tooro:
    sta 66*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_67oro:
    sta 67*ROW_STRIDE + rowBlit,x
e_68ro:
    sta 68*ROW_STRIDE + rowBlit,x
    lsr
    sta 69*ROW_STRIDE + rowBlit,x
    rts

e_r65torotoro:
    lsr
    sta 65*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 66*ROW_STRIDE + rowBlit,x
    lsr
    sta 67*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_68ro

e_r85ootoo:
    lsr
    sta 85*ROW_STRIDE + rowBlit,x
    bra e_86otoo

e_85rootoo:
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
e_86otoo:
    sta 86*ROW_STRIDE + rowBlit,x
e_87too:
    sta 87*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 88*ROW_STRIDE + rowBlit,x
    sta 89*ROW_STRIDE + rowBlit,x
    rts

e_t85rootoo:
    lda (pTex),y
    iny
    bra e_85rootoo

e_110rootoo:
    sta 110*ROW_STRIDE + rowBlit,x
    lsr
    bra e_111otoo

e_110ootoo:
    sta 110*ROW_STRIDE + rowBlit,x
e_111otoo:
    sta 111*ROW_STRIDE + rowBlit,x
e_112too:
    sta 112*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 113*ROW_STRIDE + rowBlit,x
    sta 114*ROW_STRIDE + rowBlit,x
    rts

e_r110ootoo:
    lsr
    bra e_110ootoo

e_t110orotoo:
    lda (pTex),y
    iny
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lsr
    bra e_112too

e_30tooroo:
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_31oroo:
    sta 31*ROW_STRIDE + rowBlit,x
    sta 32*ROW_STRIDE + rowBlit,x
    lsr
    sta 33*ROW_STRIDE + rowBlit,x
    sta 34*ROW_STRIDE + rowBlit,x
    rts

e_t30ooroo:
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    bra e_31oroo

e_95rootoo:
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
    bra e_96otoo

e_95ootoo:
    sta 95*ROW_STRIDE + rowBlit,x
e_96otoo:
    sta 96*ROW_STRIDE + rowBlit,x
e_97too:
    sta 97*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 98*ROW_STRIDE + rowBlit,x
    sta 99*ROW_STRIDE + rowBlit,x
    rts

e_r95ootoo:
    lsr
    bra e_95ootoo

e_t95orotoo:
    lda (pTex),y
    iny
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    bra e_97too

e_20toooro:
    sta 20*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 21*ROW_STRIDE + rowBlit,x
    bra e_22oro

e_20rotooro:
    sta 20*ROW_STRIDE + rowBlit,x
    lsr
e_21tooro:
    sta 21*ROW_STRIDE + rowBlit,x
e_t22oro:
    lda (pTex),y
    iny
e_22oro:
    sta 22*ROW_STRIDE + rowBlit,x
e_23ro:
    sta 23*ROW_STRIDE + rowBlit,x
    lsr
    sta 24*ROW_STRIDE + rowBlit,x
    rts

e_r20otooro:
    lsr
    sta 20*ROW_STRIDE + rowBlit,x
    bra e_21tooro

e_120roooto:
    sta 120*ROW_STRIDE + rowBlit,x
    lsr
    sta 121*ROW_STRIDE + rowBlit,x
    bra e_122oto

e_120orooto:
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    lsr
e_122oto:
    sta 122*ROW_STRIDE + rowBlit,x
    sta 123*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 124*ROW_STRIDE + rowBlit,x
    rts

e_t120orooto:
    lda (pTex),y
    iny
    bra e_120orooto

e_r15ootoo:
    lsr
    sta 15*ROW_STRIDE + rowBlit,x
    bra e_16otoo

e_15rootoo:
    sta 15*ROW_STRIDE + rowBlit,x
    lsr
e_16otoo:
    sta 16*ROW_STRIDE + rowBlit,x
    sta 17*ROW_STRIDE + rowBlit,x
e_t18o:
    lda (pTex),y
    iny
    sta 18*ROW_STRIDE + rowBlit,x
    sta 19*ROW_STRIDE + rowBlit,x
    rts

e_t15rootoo:
    lda (pTex),y
    iny
    bra e_15rootoo

e_20otooo:
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_22oo:
    sta 22*ROW_STRIDE + rowBlit,x
    sta 23*ROW_STRIDE + rowBlit,x
    sta 24*ROW_STRIDE + rowBlit,x
    rts

e_r20otooo:
    lsr
    bra e_20otooo

e_25orooo:
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    bra e_27oo

e_25otooo:
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_27oo:
    sta 27*ROW_STRIDE + rowBlit,x
    sta 28*ROW_STRIDE + rowBlit,x
    sta 29*ROW_STRIDE + rowBlit,x
    rts

e_r25otooo:
    lsr
    bra e_25otooo

e_60orooo:
    sta 60*ROW_STRIDE + rowBlit,x
    sta 61*ROW_STRIDE + rowBlit,x
    lsr
    bra e_62oo

e_60otooo:
    sta 60*ROW_STRIDE + rowBlit,x
    sta 61*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_62oo:
    sta 62*ROW_STRIDE + rowBlit,x
    sta 63*ROW_STRIDE + rowBlit,x
    sta 64*ROW_STRIDE + rowBlit,x
    rts

e_r60otooo:
    lsr
    bra e_60otooo

e_20tooroo:
    sta 20*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_21oroo:
    sta 21*ROW_STRIDE + rowBlit,x
e_22roo:
    sta 22*ROW_STRIDE + rowBlit,x
    lsr
    sta 23*ROW_STRIDE + rowBlit,x
    sta 24*ROW_STRIDE + rowBlit,x
    rts

e_t20ooroo:
    lda (pTex),y
    iny
    sta 20*ROW_STRIDE + rowBlit,x
    bra e_21oroo

e_r80otoroo:
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_82roo

e_80tooroo:
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_81oroo:
    sta 81*ROW_STRIDE + rowBlit,x
e_82roo:
    sta 82*ROW_STRIDE + rowBlit,x
    lsr
    sta 83*ROW_STRIDE + rowBlit,x
    sta 84*ROW_STRIDE + rowBlit,x
    rts

e_r80tooroo:
    lsr
    bra e_80tooroo

e_r95otoroo:
    lsr
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_97roo

e_95tooroo:
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_96oroo:
    sta 96*ROW_STRIDE + rowBlit,x
e_97roo:
    sta 97*ROW_STRIDE + rowBlit,x
    lsr
    sta 98*ROW_STRIDE + rowBlit,x
    sta 99*ROW_STRIDE + rowBlit,x
    rts

e_t95ooroo:
    lda (pTex),y
    iny
    sta 95*ROW_STRIDE + rowBlit,x
    bra e_96oroo

e_90rootoo:
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92too

e_90torotoo:
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_91rotoo:
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
e_92too:
    sta 92*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 93*ROW_STRIDE + rowBlit,x
    sta 94*ROW_STRIDE + rowBlit,x
    rts

e_r90ootoo:
    lsr
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    bra e_92too

e_t90orotoo:
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    bra e_91rotoo

e_r70torotoo:
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_71rotoo:
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
e_72too:
    sta 72*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 73*ROW_STRIDE + rowBlit,x
    sta 74*ROW_STRIDE + rowBlit,x
    rts

e_t70orotoo:
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    bra e_71rotoo

e_105ooroo:
    sta 105*ROW_STRIDE + rowBlit,x
e_106oroo:
    sta 106*ROW_STRIDE + rowBlit,x
e_107roo:
    sta 107*ROW_STRIDE + rowBlit,x
    lsr
    sta 108*ROW_STRIDE + rowBlit,x
    sta 109*ROW_STRIDE + rowBlit,x
    rts

e_r105tooroo:
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_106oroo

e_t105ooroo:
    lda (pTex),y
    iny
    bra e_105ooroo

e_110tooroo:
    sta 110*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_111oroo

e_110ooroo:
    sta 110*ROW_STRIDE + rowBlit,x
e_111oroo:
    sta 111*ROW_STRIDE + rowBlit,x
e_112roo:
    sta 112*ROW_STRIDE + rowBlit,x
    lsr
    sta 113*ROW_STRIDE + rowBlit,x
    sta 114*ROW_STRIDE + rowBlit,x
    rts

e_r110otoroo:
    lsr
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_112roo

e_t110ooroo:
    lda (pTex),y
    iny
    bra e_110ooroo

e_55tooroo:
    sta 55*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_56oroo

e_55ooroo:
    sta 55*ROW_STRIDE + rowBlit,x
e_56oroo:
    sta 56*ROW_STRIDE + rowBlit,x
e_57roo:
    sta 57*ROW_STRIDE + rowBlit,x
    lsr
    sta 58*ROW_STRIDE + rowBlit,x
    sta 59*ROW_STRIDE + rowBlit,x
    rts

e_t55ooroo:
    lda (pTex),y
    iny
    bra e_55ooroo

e_r100otooro:
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    bra e_101tooro

e_100rotooro:
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
e_101tooro:
    sta 101*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_102oro:
    sta 102*ROW_STRIDE + rowBlit,x
e_103ro:
    sta 103*ROW_STRIDE + rowBlit,x
    lsr
    sta 104*ROW_STRIDE + rowBlit,x
    rts

e_t100orotoro:
    lda (pTex),y
    iny
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    lsr
    sta 102*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_103ro

e_t100rootoro:
    lda (pTex),y
    iny
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
    sta 101*ROW_STRIDE + rowBlit,x
    sta 102*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_103ro

e_100orooo:
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    lsr
    bra e_102oo

e_100otooo:
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_102oo:
    sta 102*ROW_STRIDE + rowBlit,x
    sta 103*ROW_STRIDE + rowBlit,x
    sta 104*ROW_STRIDE + rowBlit,x
    rts

e_r100otooo:
    lsr
    bra e_100otooo

e_105otooo:
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_107oo:
    sta 107*ROW_STRIDE + rowBlit,x
    sta 108*ROW_STRIDE + rowBlit,x
    sta 109*ROW_STRIDE + rowBlit,x
    rts

e_115orooo:
    sta 115*ROW_STRIDE + rowBlit,x
    sta 116*ROW_STRIDE + rowBlit,x
    lsr
    bra e_117oo

e_115otooo:
    sta 115*ROW_STRIDE + rowBlit,x
    sta 116*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_117oo:
    sta 117*ROW_STRIDE + rowBlit,x
    sta 118*ROW_STRIDE + rowBlit,x
    sta 119*ROW_STRIDE + rowBlit,x
    rts

e_r115otooo:
    lsr
    bra e_115otooo

e_15orooo:
    sta 15*ROW_STRIDE + rowBlit,x
    sta 16*ROW_STRIDE + rowBlit,x
    lsr
    bra e_17oo

e_15otooo:
    sta 15*ROW_STRIDE + rowBlit,x
    sta 16*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_17oo:
    sta 17*ROW_STRIDE + rowBlit,x
    sta 18*ROW_STRIDE + rowBlit,x
    sta 19*ROW_STRIDE + rowBlit,x
    rts

e_r15otooo:
    lsr
    bra e_15otooo

e_r30ootoo:
    lsr
    sta 30*ROW_STRIDE + rowBlit,x
    bra e_31otoo

e_30rootoo:
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
e_31otoo:
    sta 31*ROW_STRIDE + rowBlit,x
e_32too:
    sta 32*ROW_STRIDE + rowBlit,x
e_t33o:
    lda (pTex),y
    iny
    sta 33*ROW_STRIDE + rowBlit,x
    sta 34*ROW_STRIDE + rowBlit,x
    rts

e_t30orotoo:
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    bra e_32too

e_55orooo:
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
    bra e_57oo

e_55otooo:
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_57oo:
    sta 57*ROW_STRIDE + rowBlit,x
    sta 58*ROW_STRIDE + rowBlit,x
    sta 59*ROW_STRIDE + rowBlit,x
    rts

e_r55otooo:
    lsr
    bra e_55otooo

e_70orooo:
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    bra e_72oo

e_70otooo:
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_72oo:
    sta 72*ROW_STRIDE + rowBlit,x
    sta 73*ROW_STRIDE + rowBlit,x
    sta 74*ROW_STRIDE + rowBlit,x
    rts

e_r70otooo:
    lsr
    bra e_70otooo

e_80orooo:
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    bra e_82oo

e_80otooo:
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_82oo:
    sta 82*ROW_STRIDE + rowBlit,x
    sta 83*ROW_STRIDE + rowBlit,x
    sta 84*ROW_STRIDE + rowBlit,x
    rts

e_65rootoo:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    bra e_66otoo

e_65ootoo:
    sta 65*ROW_STRIDE + rowBlit,x
e_66otoo:
    sta 66*ROW_STRIDE + rowBlit,x
    sta 67*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 68*ROW_STRIDE + rowBlit,x
    sta 69*ROW_STRIDE + rowBlit,x
    rts

e_r65ootoo:
    lsr
    bra e_65ootoo

e_10otooo:
    sta 10*ROW_STRIDE + rowBlit,x
    sta 11*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_12oo

e_10orooo:
    sta 10*ROW_STRIDE + rowBlit,x
    sta 11*ROW_STRIDE + rowBlit,x
    lsr
e_12oo:
    sta 12*ROW_STRIDE + rowBlit,x
    sta 13*ROW_STRIDE + rowBlit,x
    sta 14*ROW_STRIDE + rowBlit,x
    rts

e_30otooo:
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_32oo

e_30orooo:
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
e_32oo:
    sta 32*ROW_STRIDE + rowBlit,x
    sta 33*ROW_STRIDE + rowBlit,x
    sta 34*ROW_STRIDE + rowBlit,x
    rts

e_10roooto:
    sta 10*ROW_STRIDE + rowBlit,x
    lsr
    sta 11*ROW_STRIDE + rowBlit,x
    bra e_12oto

e_10orooto:
    sta 10*ROW_STRIDE + rowBlit,x
    sta 11*ROW_STRIDE + rowBlit,x
    lsr
e_12oto:
    sta 12*ROW_STRIDE + rowBlit,x
    sta 13*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 14*ROW_STRIDE + rowBlit,x
    rts

e_t10orooto:
    lda (pTex),y
    iny
    bra e_10orooto

e_110rotooro:
    sta 110*ROW_STRIDE + rowBlit,x
    lsr
e_111tooro:
    sta 111*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_112oro:
    sta 112*ROW_STRIDE + rowBlit,x
e_113ro:
    sta 113*ROW_STRIDE + rowBlit,x
    lsr
    sta 114*ROW_STRIDE + rowBlit,x
    rts

e_110toooro:
    sta 110*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 111*ROW_STRIDE + rowBlit,x
    bra e_112oro

e_r110otooro:
    lsr
    sta 110*ROW_STRIDE + rowBlit,x
    bra e_111tooro

e_115roooto:
    sta 115*ROW_STRIDE + rowBlit,x
    lsr
    sta 116*ROW_STRIDE + rowBlit,x
    bra e_117oto

e_115orooto:
    sta 115*ROW_STRIDE + rowBlit,x
    sta 116*ROW_STRIDE + rowBlit,x
    lsr
e_117oto:
    sta 117*ROW_STRIDE + rowBlit,x
    sta 118*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 119*ROW_STRIDE + rowBlit,x
    rts

e_t115orooto:
    lda (pTex),y
    iny
    bra e_115orooto

e_t10ooroo:
    lda (pTex),y
    iny
    sta 10*ROW_STRIDE + rowBlit,x
    bra e_11oroo

e_10tooroo:
    sta 10*ROW_STRIDE + rowBlit,x
e_t11oroo:
    lda (pTex),y
    iny
e_11oroo:
    sta 11*ROW_STRIDE + rowBlit,x
    sta 12*ROW_STRIDE + rowBlit,x
    lsr
    sta 13*ROW_STRIDE + rowBlit,x
    sta 14*ROW_STRIDE + rowBlit,x
    rts

e_120ooroo:
    sta 120*ROW_STRIDE + rowBlit,x
e_121oroo:
    sta 121*ROW_STRIDE + rowBlit,x
    sta 122*ROW_STRIDE + rowBlit,x
    lsr
    sta 123*ROW_STRIDE + rowBlit,x
    sta 124*ROW_STRIDE + rowBlit,x
    rts

e_t120ooroo:
    lda (pTex),y
    iny
    bra e_120ooroo

e_t5ooroo:
    lda (pTex),y
    iny
    sta 5*ROW_STRIDE + rowBlit,x
    bra e_6oroo

e_5tooroo:
    sta 5*ROW_STRIDE + rowBlit,x
e_t6oroo:
    lda (pTex),y
    iny
e_6oroo:
    sta 6*ROW_STRIDE + rowBlit,x
    sta 7*ROW_STRIDE + rowBlit,x
    lsr
    sta 8*ROW_STRIDE + rowBlit,x
    sta 9*ROW_STRIDE + rowBlit,x
    rts

e_r60otooro:
    lsr
    sta 60*ROW_STRIDE + rowBlit,x
    sta 61*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_62oro

e_60toooro:
    sta 60*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_61ooro:
    sta 61*ROW_STRIDE + rowBlit,x
e_62oro:
    sta 62*ROW_STRIDE + rowBlit,x
e_63ro:
    sta 63*ROW_STRIDE + rowBlit,x
    lsr
    sta 64*ROW_STRIDE + rowBlit,x
    rts

e_r100ootoo:
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    bra e_101otoo

e_100rootoo:
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
e_101otoo:
    sta 101*ROW_STRIDE + rowBlit,x
e_102too:
    sta 102*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 103*ROW_STRIDE + rowBlit,x
    sta 104*ROW_STRIDE + rowBlit,x
    rts

e_t100orotoo:
    lda (pTex),y
    iny
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    lsr
    bra e_102too

e_r105ootoo:
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    bra e_106otoo

e_105rootoo:
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
e_106otoo:
    sta 106*ROW_STRIDE + rowBlit,x
e_107too:
    sta 107*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 108*ROW_STRIDE + rowBlit,x
    sta 109*ROW_STRIDE + rowBlit,x
    rts

e_t105orotoo:
    lda (pTex),y
    iny
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    bra e_107too

e_120otooo:
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_122oo:
    sta 122*ROW_STRIDE + rowBlit,x
    sta 123*ROW_STRIDE + rowBlit,x
    sta 124*ROW_STRIDE + rowBlit,x
    rts

e_35otooo:
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_37oo:
    sta 37*ROW_STRIDE + rowBlit,x
    sta 38*ROW_STRIDE + rowBlit,x
    sta 39*ROW_STRIDE + rowBlit,x
    rts

e_r35otooo:
    lsr
    bra e_35otooo

e_40otooo:
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_42oo:
    sta 42*ROW_STRIDE + rowBlit,x
    sta 43*ROW_STRIDE + rowBlit,x
    sta 44*ROW_STRIDE + rowBlit,x
    rts

e_45orooo:
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    bra e_47oo

e_45otooo:
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_47oo:
    sta 47*ROW_STRIDE + rowBlit,x
    sta 48*ROW_STRIDE + rowBlit,x
    sta 49*ROW_STRIDE + rowBlit,x
    rts

e_75otooo:
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_77oo:
    sta 77*ROW_STRIDE + rowBlit,x
    sta 78*ROW_STRIDE + rowBlit,x
    sta 79*ROW_STRIDE + rowBlit,x
    rts

e_r75otooo:
    lsr
    bra e_75otooo

e_r10ootoo:
    lsr
    sta 10*ROW_STRIDE + rowBlit,x
    bra e_11otoo

e_10rootoo:
    sta 10*ROW_STRIDE + rowBlit,x
    lsr
e_11otoo:
    sta 11*ROW_STRIDE + rowBlit,x
    sta 12*ROW_STRIDE + rowBlit,x
e_t13o:
    lda (pTex),y
    iny
    sta 13*ROW_STRIDE + rowBlit,x
    sta 14*ROW_STRIDE + rowBlit,x
    rts

e_5rootoo:
    sta 5*ROW_STRIDE + rowBlit,x
    lsr
    bra e_6otoo

e_5ootoo:
    sta 5*ROW_STRIDE + rowBlit,x
e_6otoo:
    sta 6*ROW_STRIDE + rowBlit,x
    sta 7*ROW_STRIDE + rowBlit,x
e_t8o:
    lda (pTex),y
    iny
    sta 8*ROW_STRIDE + rowBlit,x
    sta 9*ROW_STRIDE + rowBlit,x
    rts

e_r115otooro:
    lsr
    sta 115*ROW_STRIDE + rowBlit,x
    sta 116*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_117oro

e_115toooro:
    sta 115*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_116ooro:
    sta 116*ROW_STRIDE + rowBlit,x
e_117oro:
    sta 117*ROW_STRIDE + rowBlit,x
    sta 118*ROW_STRIDE + rowBlit,x
    lsr
    sta 119*ROW_STRIDE + rowBlit,x
    rts

e_r120otooro:
    lsr
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_122oro

e_120toooro:
    sta 120*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_121ooro:
    sta 121*ROW_STRIDE + rowBlit,x
e_122oro:
    sta 122*ROW_STRIDE + rowBlit,x
    sta 123*ROW_STRIDE + rowBlit,x
    lsr
    sta 124*ROW_STRIDE + rowBlit,x
    rts

e_65rotoroo:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_67roo

e_65tooroo:
    sta 65*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_66oroo:
    sta 66*ROW_STRIDE + rowBlit,x
e_67roo:
    sta 67*ROW_STRIDE + rowBlit,x
    lsr
    sta 68*ROW_STRIDE + rowBlit,x
    sta 69*ROW_STRIDE + rowBlit,x
    rts

e_t65ooroo:
    lda (pTex),y
    iny
    sta 65*ROW_STRIDE + rowBlit,x
    bra e_66oroo

e_75tooroo:
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_76oroo:
    sta 76*ROW_STRIDE + rowBlit,x
e_77roo:
    sta 77*ROW_STRIDE + rowBlit,x
    lsr
    sta 78*ROW_STRIDE + rowBlit,x
    sta 79*ROW_STRIDE + rowBlit,x
    rts

e_t75ooroo:
    lda (pTex),y
    iny
    sta 75*ROW_STRIDE + rowBlit,x
    bra e_76oroo

e_10toooro:
    sta 10*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 11*ROW_STRIDE + rowBlit,x
    bra e_12oro

e_r10otooro:
    lsr
    sta 10*ROW_STRIDE + rowBlit,x
    sta 11*ROW_STRIDE + rowBlit,x
e_t12oro:
    lda (pTex),y
    iny
e_12oro:
    sta 12*ROW_STRIDE + rowBlit,x
    sta 13*ROW_STRIDE + rowBlit,x
    lsr
    sta 14*ROW_STRIDE + rowBlit,x
    rts

e_5toooro:
    sta 5*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 6*ROW_STRIDE + rowBlit,x
    bra e_7oro

e_r5otooro:
    lsr
    sta 5*ROW_STRIDE + rowBlit,x
    sta 6*ROW_STRIDE + rowBlit,x
e_t7oro:
    lda (pTex),y
    iny
e_7oro:
    sta 7*ROW_STRIDE + rowBlit,x
    sta 8*ROW_STRIDE + rowBlit,x
    lsr
    sta 9*ROW_STRIDE + rowBlit,x
    rts

e_110tooroto:
    sta 110*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 111*ROW_STRIDE + rowBlit,x
    sta 112*ROW_STRIDE + rowBlit,x
    lsr
    bra e_113to

e_110roooto:
    sta 110*ROW_STRIDE + rowBlit,x
    lsr
    sta 111*ROW_STRIDE + rowBlit,x
e_112oto:
    sta 112*ROW_STRIDE + rowBlit,x
e_113to:
    sta 113*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 114*ROW_STRIDE + rowBlit,x
    rts

e_t110orooto:
    lda (pTex),y
    iny
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lsr
    bra e_112oto

e_50otooo:
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_52oo

e_50roooo:
    sta 50*ROW_STRIDE + rowBlit,x
    lsr
e_51ooo:
    sta 51*ROW_STRIDE + rowBlit,x
e_52oo:
    sta 52*ROW_STRIDE + rowBlit,x
    sta 53*ROW_STRIDE + rowBlit,x
    sta 54*ROW_STRIDE + rowBlit,x
    rts

e_65roooto:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    bra e_67oto

e_65orooto:
    sta 65*ROW_STRIDE + rowBlit,x
    sta 66*ROW_STRIDE + rowBlit,x
    lsr
e_67oto:
    sta 67*ROW_STRIDE + rowBlit,x
e_68to:
    sta 68*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 69*ROW_STRIDE + rowBlit,x
    rts

e_65rotoroto:
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 67*ROW_STRIDE + rowBlit,x
    lsr
    bra e_68to

e_125oo:
    sta 125*ROW_STRIDE + rowBlit,x
    sta 126*ROW_STRIDE + rowBlit,x
    sta 127*ROW_STRIDE + rowBlit,x
    rts

e_t125oo:
    lda (pTex),y
    iny
    bra e_125oo

e_65orooo:
    sta 65*ROW_STRIDE + rowBlit,x
    sta 66*ROW_STRIDE + rowBlit,x
    lsr
e_67oo:
    sta 67*ROW_STRIDE + rowBlit,x
    sta 68*ROW_STRIDE + rowBlit,x
    sta 69*ROW_STRIDE + rowBlit,x
    rts

e_85otooo:
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_87oo

e_85orooo:
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
e_87oo:
    sta 87*ROW_STRIDE + rowBlit,x
    sta 88*ROW_STRIDE + rowBlit,x
    sta 89*ROW_STRIDE + rowBlit,x
    rts

e_115rootoo:
    sta 115*ROW_STRIDE + rowBlit,x
    lsr
e_116otoo:
    sta 116*ROW_STRIDE + rowBlit,x
    sta 117*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 118*ROW_STRIDE + rowBlit,x
    sta 119*ROW_STRIDE + rowBlit,x
    rts

e_60rootoo:
    sta 60*ROW_STRIDE + rowBlit,x
    lsr
e_61otoo:
    sta 61*ROW_STRIDE + rowBlit,x
    sta 62*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 63*ROW_STRIDE + rowBlit,x
    sta 64*ROW_STRIDE + rowBlit,x
    rts

e_r60ootoo:
    lsr
    sta 60*ROW_STRIDE + rowBlit,x
    bra e_61otoo

e_115tooroo:
    sta 115*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_116oroo:
    sta 116*ROW_STRIDE + rowBlit,x
    sta 117*ROW_STRIDE + rowBlit,x
    lsr
    sta 118*ROW_STRIDE + rowBlit,x
    sta 119*ROW_STRIDE + rowBlit,x
    rts

e_t115ooroo:
    lda (pTex),y
    iny
    sta 115*ROW_STRIDE + rowBlit,x
    bra e_116oroo

e_60tooroo:
    sta 60*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_61oroo:
    sta 61*ROW_STRIDE + rowBlit,x
    sta 62*ROW_STRIDE + rowBlit,x
    lsr
    sta 63*ROW_STRIDE + rowBlit,x
    sta 64*ROW_STRIDE + rowBlit,x
    rts

e_t60ooroo:
    lda (pTex),y
    iny
    sta 60*ROW_STRIDE + rowBlit,x
    bra e_61oroo

e_t0ooroo:
    lda (pTex),y
    iny
    sta 0*ROW_STRIDE + rowBlit,x
e_1oroo:
    sta 1*ROW_STRIDE + rowBlit,x
    sta 2*ROW_STRIDE + rowBlit,x
    lsr
    sta 3*ROW_STRIDE + rowBlit,x
    sta 4*ROW_STRIDE + rowBlit,x
    rts

e_125oto:
    sta 125*ROW_STRIDE + rowBlit,x
    sta 126*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 127*ROW_STRIDE + rowBlit,x
    rts

e_r125oto:
    lsr
    bra e_125oto

e_t0oooro:
    lda (pTex),y
    iny
    sta 0*ROW_STRIDE + rowBlit,x
    sta 1*ROW_STRIDE + rowBlit,x
e_2oro:
    sta 2*ROW_STRIDE + rowBlit,x
    sta 3*ROW_STRIDE + rowBlit,x
    lsr
    sta 4*ROW_STRIDE + rowBlit,x
    rts

e_110orooo:
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lsr
e_112oo:
    sta 112*ROW_STRIDE + rowBlit,x
    sta 113*ROW_STRIDE + rowBlit,x
    sta 114*ROW_STRIDE + rowBlit,x
    rts

e_110otooo:
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    bra e_112oo

e_5otooo:
    sta 5*ROW_STRIDE + rowBlit,x
    sta 6*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
e_7oo:
    sta 7*ROW_STRIDE + rowBlit,x
    sta 8*ROW_STRIDE + rowBlit,x
    sta 9*ROW_STRIDE + rowBlit,x
    rts

e_95orooo:
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
e_97oo:
    sta 97*ROW_STRIDE + rowBlit,x
    sta 98*ROW_STRIDE + rowBlit,x
    sta 99*ROW_STRIDE + rowBlit,x
    rts

; Produce 2 rows from 2 rows
expand_2:
    jsr selectMip5
    lda (pTex),y
    iny
    jsr e_63ro
expand_0:
    rts

; Produce 4 rows from 4 rows
expand_4:
    jsr selectMip4
    jsr e_t62roto
    lsr
    sta 65*ROW_STRIDE + rowBlit,x
    rts

; Produce 6 rows from 4 rows
expand_6:
    jsr selectMip4
    lda (pTex),y
    iny
    sta 61*ROW_STRIDE + rowBlit,x
    jsr e_62roto
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    rts

; Produce 8 rows from 8 rows
expand_8:
    jsr selectMip3
    jsr e_t60rotoroto
    lsr
    sta 65*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 66*ROW_STRIDE + rowBlit,x
    lsr
    sta 67*ROW_STRIDE + rowBlit,x
    rts

; Produce 10 rows from 8 rows
expand_10:
    jsr selectMip3
    lda (pTex),y
    iny
    sta 59*ROW_STRIDE + rowBlit,x
    jsr e_60rotoroto
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 67*ROW_STRIDE + rowBlit,x
    lsr
    sta 68*ROW_STRIDE + rowBlit,x
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
    jsr e_t57oro
    jsr e_60tooroto
    jsr e_65rootoo
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    rts

; Produce 16 rows from 16 rows
expand_16:
    jsr selectMip2
    lda (pTex),y
    iny
    sta 56*ROW_STRIDE + rowBlit,x
    lsr
    sta 57*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_58ro
    jsr e_t60rotoroto
    jsr e_r65torotoro
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    rts

; Produce 18 rows from 16 rows
expand_18:
    jsr selectMip2
    jsr e_t55orotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    sta 72*ROW_STRIDE + rowBlit,x
    rts

; Produce 20 rows from 16 rows
expand_20:
    jsr selectMip2
    lda (pTex),y
    iny
    sta 54*ROW_STRIDE + rowBlit,x
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroto
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 72*ROW_STRIDE + rowBlit,x
    lsr
    sta 73*ROW_STRIDE + rowBlit,x
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
    sta 75*ROW_STRIDE + rowBlit,x
    rts

; Produce 26 rows from 16 rows
expand_26:
    jsr selectMip2
    lda (pTex),y
    iny
    jsr e_51oroo
    jsr e_t55rootoo
    jsr e_r60tooroto
    jsr e_65rootoro
    jsr e_70tooroto
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
    sta 76*ROW_STRIDE + rowBlit,x
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
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    sta 77*ROW_STRIDE + rowBlit,x
    rts

; Produce 30 rows from 16 rows
expand_30:
    jsr selectMip2
    lda (pTex),y
    iny
    sta 49*ROW_STRIDE + rowBlit,x
    jsr e_50rootoo
    jsr e_r55otooro
    jsr e_60tooroto
    jsr e_65rootoo
    jsr e_r70otooro
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    sta 77*ROW_STRIDE + rowBlit,x
    lsr
    sta 78*ROW_STRIDE + rowBlit,x
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
    sta 80*ROW_STRIDE + rowBlit,x
    rts

; Produce 36 rows from 32 rows
expand_36:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
    jsr e_47roto
    jsr e_r50torotoro
    jsr e_t55orotoro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_r70torotoo
    jsr e_r75torotoro
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
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
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    rts

; Produce 40 rows from 32 rows
expand_40:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 44*ROW_STRIDE + rowBlit,x
    jsr e_45rotoroto
    jsr e_50rotoroto
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroto
    jsr e_70rotoroto
    jsr e_75rotoroto
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 82*ROW_STRIDE + rowBlit,x
    lsr
    sta 83*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
    sta 87*ROW_STRIDE + rowBlit,x
    rts

; Produce 50 rows from 32 rows
expand_50:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 39*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    sta 88*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    sta 92*ROW_STRIDE + rowBlit,x
    rts

; Produce 60 rows from 32 rows
expand_60:
    jsr selectMip1
    lda (pTex),y
    iny
    sta 34*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    sta 92*ROW_STRIDE + rowBlit,x
    lsr
    sta 93*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
    sta 96*ROW_STRIDE + rowBlit,x
    rts

; Produce 68 rows from 64 rows
expand_68:
    jsr selectMip0
    jsr e_t30orotoro
    lda (pTex),y
    iny
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 37*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    rts

; Produce 70 rows from 64 rows
expand_70:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 29*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
    sta 96*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 97*ROW_STRIDE + rowBlit,x
    lsr
    sta 98*ROW_STRIDE + rowBlit,x
    rts

; Produce 72 rows from 64 rows
expand_72:
    jsr selectMip0
    jsr e_t28o
    lsr
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
    lsr
    sta 32*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
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
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
    sta 31*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_32roto
    jsr e_35rotoroto
    jsr e_r40tooroto
    jsr e_r45torotoro
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_53ro
    lda (pTex),y
    iny
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
    sta 56*ROW_STRIDE + rowBlit,x
    sta 57*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_58ro
    jsr e_t60rotoroto
    jsr e_65rotoroto
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    jsr e_72roto
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_77too
    jsr e_r80torotoro
    jsr e_t85rootoro
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_92roo
    lda (pTex),y
    iny
    jsr e_95rotoroto
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
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
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
    sta 101*ROW_STRIDE + rowBlit,x
    rts

; Produce 78 rows from 64 rows
expand_78:
    jsr selectMip0
    jsr e_t25orotoro
    jsr e_t30orotoro
    jsr e_t35rootoro
    lda (pTex),y
    iny
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
    sta 42*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_43ro
    jsr e_t45rotooro
    lda (pTex),y
    iny
    jsr e_50rotooro
    lda (pTex),y
    iny
    sta 55*ROW_STRIDE + rowBlit,x
    lsr
    sta 56*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_57roo
    jsr e_t60rotoroto
    jsr e_65rotoroto
    jsr e_70rotoroto
    lsr
    sta 75*ROW_STRIDE + rowBlit,x
    jsr e_76toroto
    lsr
    sta 80*ROW_STRIDE + rowBlit,x
    jsr e_81toroto
    jsr e_r85tooroto
    jsr e_r90tooroto
    jsr e_r95torooto
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 101*ROW_STRIDE + rowBlit,x
    lsr
    sta 102*ROW_STRIDE + rowBlit,x
    rts

; Produce 80 rows from 64 rows
expand_80:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 24*ROW_STRIDE + rowBlit,x
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    sta 26*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_27roto
    jsr e_30rotoroto
    jsr e_35rotoroto
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_88to
    jsr e_90rotoroto
    jsr e_95rotoroto
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
    sta 101*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 102*ROW_STRIDE + rowBlit,x
    lsr
    sta 103*ROW_STRIDE + rowBlit,x
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
    sta 45*ROW_STRIDE + rowBlit,x
    jsr e_46toroto
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    jsr e_51toroto
    jsr e_55rotoroto
    jsr e_60rotoroto
    jsr e_65rotoroo
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_72roo
    lda (pTex),y
    iny
    jsr e_75rotooro
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
    sta 40*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 41*ROW_STRIDE + rowBlit,x
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
    jsr e_90torotoo
    jsr e_r95torooto
    jsr e_r100tooroto
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    rts

; Produce 86 rows from 64 rows
expand_86:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 21*ROW_STRIDE + rowBlit,x
    sta 22*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_23to
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    sta 26*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_27roo
    lda (pTex),y
    iny
    jsr e_30rotooro
    jsr e_t35rootoro
    jsr e_t40orotoro
    sta 45*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 46*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    jsr e_96toroto
    sta 100*ROW_STRIDE + rowBlit,x
    lsr
    sta 101*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_102roo
    lda (pTex),y
    iny
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
    sta 106*ROW_STRIDE + rowBlit,x
    rts

; Produce 88 rows from 64 rows
expand_88:
    jsr selectMip0
    jsr e_t20orotoo
    jsr e_r25torooto
    jsr e_r30tooroto
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_92roo
    lda (pTex),y
    iny
    jsr e_95rotooro
    jsr e_t100orotoro
    sta 105*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    sta 107*ROW_STRIDE + rowBlit,x
    rts

; Produce 90 rows from 64 rows
expand_90:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 19*ROW_STRIDE + rowBlit,x
    jsr e_20rotooro
    jsr e_t25orotoro
    jsr e_30torooto
    jsr e_r35tooroto
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_42roo
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    lsr
    sta 46*ROW_STRIDE + rowBlit,x
    sta 47*ROW_STRIDE + rowBlit,x
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
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    sta 72*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_73ro
    jsr e_75torooto
    jsr e_r80tooroto
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_87roo
    jsr e_t90rootoro
    jsr e_t95orotoo
    lsr
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_101rooto
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 107*ROW_STRIDE + rowBlit,x
    lsr
    sta 108*ROW_STRIDE + rowBlit,x
    rts

; Produce 92 rows from 64 rows
expand_92:
    jsr selectMip0
    jsr e_t18o
    lsr
    sta 20*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 21*ROW_STRIDE + rowBlit,x
    sta 22*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_23to
    jsr e_25rotooro
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
    sta 31*ROW_STRIDE + rowBlit,x
    sta 32*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 87*ROW_STRIDE + rowBlit,x
    jsr e_88ro
    jsr e_t90orotoo
    lsr
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    jsr e_97roto
    jsr e_r100otoroo
    lda (pTex),y
    iny
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
    sta 106*ROW_STRIDE + rowBlit,x
    sta 107*ROW_STRIDE + rowBlit,x
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
    sta 40*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    sta 42*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    sta 92*ROW_STRIDE + rowBlit,x
    jsr e_93to
    jsr e_r95otoroo
    jsr e_t100rootoro
    jsr e_105torooto
    lsr
    sta 110*ROW_STRIDE + rowBlit,x
    rts

; Produce 96 rows from 64 rows
expand_96:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 16*ROW_STRIDE + rowBlit,x
    sta 17*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_18to
    jsr e_20rotooro
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
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
    sta 85*ROW_STRIDE + rowBlit,x
    sta 86*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_87too
    jsr e_r90tooroto
    jsr e_95rotooro
    jsr e_t100orotoo
    lsr
    jsr e_105tooroto
    sta 110*ROW_STRIDE + rowBlit,x
    lsr
    sta 111*ROW_STRIDE + rowBlit,x
    rts

; Produce 98 rows from 64 rows
expand_98:
    jsr selectMip0
    jsr e_t15orooto
    lsr
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_22roo
    jsr e_t25rootoro
    jsr e_30torooto
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
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
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_77roo
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    lsr
    sta 81*ROW_STRIDE + rowBlit,x
    sta 82*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_83ro
    jsr e_85torooto
    jsr e_90rotooro
    jsr e_t95orotoo
    jsr e_r100tooroto
    jsr e_105rotooro
    lda (pTex),y
    iny
    sta 110*ROW_STRIDE + rowBlit,x
    sta 111*ROW_STRIDE + rowBlit,x
    lsr
    sta 112*ROW_STRIDE + rowBlit,x
    rts

; Produce 100 rows from 64 rows
expand_100:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 14*ROW_STRIDE + rowBlit,x
    sta 15*ROW_STRIDE + rowBlit,x
    lsr
    sta 16*ROW_STRIDE + rowBlit,x
    sta 17*ROW_STRIDE + rowBlit,x
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
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    jsr e_92toro
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    jsr e_98to
    jsr e_r100otooro
    jsr e_t105orotoo
    lsr
    sta 110*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 111*ROW_STRIDE + rowBlit,x
    sta 112*ROW_STRIDE + rowBlit,x
    lsr
    sta 113*ROW_STRIDE + rowBlit,x
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
    sta 30*ROW_STRIDE + rowBlit,x
    lsr
    sta 31*ROW_STRIDE + rowBlit,x
    sta 32*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_33ro
    jsr e_35torooto
    jsr e_40rotooro
    jsr e_t45orooto
    lsr
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
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
    jsr e_t12oro
    sta 15*ROW_STRIDE + rowBlit,x
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
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_107roo
    jsr e_t110orotoo
    lsr
    sta 115*ROW_STRIDE + rowBlit,x
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
    sta 35*ROW_STRIDE + rowBlit,x
    lsr
    sta 36*ROW_STRIDE + rowBlit,x
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
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
    sta 76*ROW_STRIDE + rowBlit,x
    sta 77*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_78ro
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    jsr e_82roto
    jsr e_85rootoro
    jsr e_90tooroto
    sta 95*ROW_STRIDE + rowBlit,x
    lsr
    sta 96*ROW_STRIDE + rowBlit,x
    sta 97*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_98ro
    jsr e_100tooroto
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
    sta 106*ROW_STRIDE + rowBlit,x
    sta 107*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_108ro
    jsr e_110tooroto
    sta 115*ROW_STRIDE + rowBlit,x
    lsr
    sta 116*ROW_STRIDE + rowBlit,x
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
    sta 115*ROW_STRIDE + rowBlit,x
    sta 116*ROW_STRIDE + rowBlit,x
    lsr
    sta 117*ROW_STRIDE + rowBlit,x
    rts

; Produce 110 rows from 64 rows
expand_110:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 9*ROW_STRIDE + rowBlit,x
    jsr e_10rootoo
    lsr
    jsr e_15tooroo
    jsr e_t20orotoo
    lsr
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_27roo
    jsr e_t30orooto
    jsr e_r35otooro
    jsr e_t40orooto
    jsr e_45rotooro
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 51*ROW_STRIDE + rowBlit,x
    lsr
    sta 52*ROW_STRIDE + rowBlit,x
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
    sta 110*ROW_STRIDE + rowBlit,x
    lsr
    sta 111*ROW_STRIDE + rowBlit,x
    sta 112*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_113ro
    sta 115*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 116*ROW_STRIDE + rowBlit,x
    sta 117*ROW_STRIDE + rowBlit,x
    lsr
    sta 118*ROW_STRIDE + rowBlit,x
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
    sta 95*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 96*ROW_STRIDE + rowBlit,x
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
    lsr
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_72roo
    jsr e_t75orooto
    jsr e_80rotooro
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    sta 87*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_88to
    jsr e_90rootoo
    jsr e_r95otoroo
    jsr e_t100orooto
    jsr e_r105otooro
    jsr e_110tooroto
    jsr e_115rootoo
    lsr
    sta 120*ROW_STRIDE + rowBlit,x
    rts

; Produce 116 rows from 64 rows
expand_116:
    jsr selectMip0
    jsr e_t6oroo
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
    sta 120*ROW_STRIDE + rowBlit,x
    lsr
    sta 121*ROW_STRIDE + rowBlit,x
    rts

; Produce 118 rows from 64 rows
expand_118:
    jsr selectMip0
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_r15otoroo
    jsr e_t20orooto
    sta 25*ROW_STRIDE + rowBlit,x
    lsr
    sta 26*ROW_STRIDE + rowBlit,x
    sta 27*ROW_STRIDE + rowBlit,x
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
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    sta 107*ROW_STRIDE + rowBlit,x
    jsr e_108to
    jsr e_110rotooro
    jsr e_115tooroo
    lda (pTex),y
    iny
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    lsr
    sta 122*ROW_STRIDE + rowBlit,x
    rts

; Produce 120 rows from 64 rows
expand_120:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 4*ROW_STRIDE + rowBlit,x
    jsr e_5rootoo
    jsr e_r10otooro
    sta 15*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 16*ROW_STRIDE + rowBlit,x
    sta 17*ROW_STRIDE + rowBlit,x
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
    sta 120*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 121*ROW_STRIDE + rowBlit,x
    sta 122*ROW_STRIDE + rowBlit,x
    lsr
    sta 123*ROW_STRIDE + rowBlit,x
    rts

; Produce 122 rows from 64 rows
expand_122:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 3*ROW_STRIDE + rowBlit,x
    sta 4*ROW_STRIDE + rowBlit,x
    jsr e_r5otooro
    jsr e_10tooroo
    jsr e_t15orooto
    sta 20*ROW_STRIDE + rowBlit,x
    lsr
    sta 21*ROW_STRIDE + rowBlit,x
    sta 22*ROW_STRIDE + rowBlit,x
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
    lda (pTex),y
    iny
    jsr e_2oro
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
    sta 125*ROW_STRIDE + rowBlit,x
    rts

; Produce 126 rows from 64 rows
expand_126:
    jsr selectMip0
    lda (pTex),y
    iny
    jsr e_1oroo
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
    sta 125*ROW_STRIDE + rowBlit,x
    lsr
    sta 126*ROW_STRIDE + rowBlit,x
    rts

; Produce 128 rows from 64 rows
expand_128:
    jsr selectMip0
    lda (pTex),y
    iny
    sta 0*ROW_STRIDE + rowBlit,x
    sta 1*ROW_STRIDE + rowBlit,x
    lsr
    sta 2*ROW_STRIDE + rowBlit,x
    sta 3*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 4*ROW_STRIDE + rowBlit,x
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
    jsr e_t0ooroo
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_r15otooro
    jsr e_20tooroo
    jsr e_t25orooto
    jsr e_30rootoo
    jsr e_35rootoo
    jsr e_r40otooro
    jsr e_45tooroo
    jsr e_t50orooto
    jsr e_55rootoo
    jsr e_r60otooro
    jsr e_65toooro
    jsr e_70tooroo
    jsr e_t75orooto
    jsr e_80rootoo
    jsr e_r85otooro
    jsr e_90tooroo
    jsr e_t95orooto
    jsr e_100orooto
    jsr e_105rootoo
    jsr e_r110otooro
    jsr e_115tooroo
    jsr e_t120orooto
    jmp e_125roo

; Produce 136 rows from 64 rows
expand_136:
    jsr selectMip0
    jsr e_t0ooroo
    jsr e_t5orooto
    jsr e_10rootoo
    jsr e_r15otooo
    jsr e_r20otooro
    jsr e_25tooroo
    jsr e_t30orooto
    jsr e_35orooto
    jsr e_40rootoo
    jsr e_r45otooro
    jsr e_50toooro
    jsr e_55tooroo
    jsr e_t60orooto
    jsr e_65rootoo
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    jsr e_72too
    jsr e_r75otooro
    jsr e_80tooroo
    jsr e_t85ooroo
    jsr e_t90orooto
    jsr e_95rootoo
    jsr e_r100otooo
    jsr e_r105otooro
    jsr e_110tooroo
    jsr e_t115orooto
    jsr e_120orooto
    jmp e_125roo

; Produce 140 rows from 64 rows
expand_140:
    jsr selectMip0
    ldy #1
    jsr e_t0ooroo
    jsr e_t5orooto
    jsr e_10roooto
    jsr e_15rootoo
    jsr e_r20otooo
    jsr e_r25otooro
    jsr e_30tooroo
    jsr e_t35ooroo
    jsr e_t40orooto
    jsr e_45roooto
    jsr e_50rootoo
    jsr e_r55otooo
    jsr e_r60otooro
    jsr e_65tooroo
    jsr e_t70ooroo
    jsr e_t75orooto
    jsr e_80roooto
    jsr e_85rootoo
    jsr e_r90otooo
    jsr e_r95otooro
    jsr e_100tooroo
    jsr e_t105ooroo
    jsr e_t110orooto
    jsr e_115roooto
    jsr e_120rootoo
    jmp e_r125oto

; Produce 144 rows from 64 rows
expand_144:
    jsr selectMip0
    ldy #1
    jsr e_t0ooroo
    jsr e_t5orooto
    jsr e_10orooto
    jsr e_15rootoo
    jsr e_20rootoo
    jsr e_r25otooo
    jsr e_r30otooro
    jsr e_35toooro
    jsr e_40tooroo
    jsr e_t45ooroo
    jsr e_t50orooto
    jsr e_55orooto
    jsr e_60rootoo
    jsr e_65rootoo
    jsr e_r70otooo
    jsr e_r75otooro
    jsr e_80toooro
    jsr e_85tooroo
    jsr e_t90ooroo
    jsr e_t95orooto
    jsr e_100orooto
    jsr e_105rootoo
    jsr e_110rootoo
    jsr e_r115otooo
    jsr e_r120otooro
    jmp e_125too

; Produce 148 rows from 64 rows
expand_148:
    jsr selectMip0
    ldy #2
    jsr e_t0ooroo
    lda (pTex),y
    iny
    sta 5*ROW_STRIDE + rowBlit,x
    sta 6*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_7oo
    jsr e_t10orooto
    jsr e_15orooto
    jsr e_20roooto
    jsr e_25rootoo
    jsr e_30rootoo
    jsr e_r35otooo
    jsr e_r40otooro
    jsr e_45otooro
    jsr e_50toooro
    jsr e_55tooroo
    jsr e_60tooroo
    jsr e_t65ooroo
    lda (pTex),y
    iny
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    lsr
    sta 72*ROW_STRIDE + rowBlit,x
    jsr e_73to
    jsr e_75orooto
    jsr e_80roooto
    jsr e_85rootoo
    jsr e_90rootoo
    jsr e_r95ootoo
    jsr e_r100otooo
    jsr e_r105otooro
    jsr e_110toooro
    jsr e_115tooroo
    sta 120*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_121oroo
    jmp e_t125oo

; Produce 152 rows from 64 rows
expand_152:
    jsr selectMip0
    ldy #2
    jsr e_t0ooroo
    jsr e_t5ooroo
    lda (pTex),y
    iny
    jsr e_10orooo
    jsr e_t15orooto
    jsr e_20orooto
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    sta 27*ROW_STRIDE + rowBlit,x
    jsr e_28to
    jsr e_30roooto
    jsr e_35rootoo
    jsr e_40rootoo
    jsr e_45rootoo
    jsr e_r50ootoo
    jsr e_r55otooo
    jsr e_r60otooo
    lsr
    sta 65*ROW_STRIDE + rowBlit,x
    jsr e_66tooro
    jsr e_70otooro
    sta 75*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 76*ROW_STRIDE + rowBlit,x
    jsr e_77oro
    jsr e_80toooro
    jsr e_85tooroo
    jsr e_90tooroo
    jsr e_t95ooroo
    jsr e_t100ooroo
    lda (pTex),y
    iny
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_107oo
    jsr e_t110orooto
    jsr e_115orooto
    jsr e_120orooto
    jmp e_125roo

; Produce 156 rows from 64 rows
expand_156:
    jsr selectMip0
    ldy #2
    jsr e_t0ooroo
    jsr e_t5ooroo
    jsr e_t10ooroo
    jsr e_t15ooroo
    lda (pTex),y
    iny
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_22oo
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_27oo
    lda (pTex),y
    iny
    jsr e_30orooo
    jsr e_t35orooto
    jsr e_40orooto
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    lsr
    sta 47*ROW_STRIDE + rowBlit,x
    jsr e_48to
    jsr e_50orooto
    jsr e_55orooto
    jsr e_60roooto
    jsr e_65roooto
    jsr e_70roooto
    jsr e_75rootoo
    jsr e_80rootoo
    jsr e_85rootoo
    jsr e_90rootoo
    jsr e_95rootoo
    jsr e_r100ootoo
    jsr e_r105ootoo
    jsr e_r110ootoo
    jsr e_r115otooo
    lsr
    jsr e_120otooo
    jmp e_r125oto

; Produce 160 rows from 64 rows
expand_160:
    jsr selectMip0
    ldy #3
    jsr e_t0ooroo
    jsr e_t5ooroo
    jsr e_t10ooroo
    jsr e_t15ooroo
    jsr e_t20ooroo
    lda (pTex),y
    iny
    jsr e_25ooroo
    jsr e_t30ooroo
    jsr e_t35ooroo
    jsr e_t40ooroo
    jsr e_t45ooroo
    jsr e_t50ooroo
    jsr e_t55ooroo
    jsr e_t60ooroo
    jsr e_t65ooroo
    jsr e_t70ooroo
    jsr e_t75ooroo
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    jsr e_81oroo
    jsr e_t85ooroo
    jsr e_t90ooroo
    jsr e_t95ooroo
    jsr e_t100ooroo
    jsr e_t105ooroo
    jsr e_t110ooroo
    jsr e_t115ooroo
    jsr e_t120ooroo
    jmp e_t125oo

; Produce 164 rows from 64 rows
expand_164:
    jsr selectMip0
    ldy #3
    jsr e_t0ooroo
    jsr e_5tooroo
    jsr e_10tooroo
    jsr e_15tooroo
    jsr e_20toooro
    sta 25*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 26*ROW_STRIDE + rowBlit,x
    jsr e_27oro
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
    jsr e_32oro
    jsr e_35toooro
    jsr e_40toooro
    jsr e_45otooro
    sta 50*ROW_STRIDE + rowBlit,x
    jsr e_51tooro
    sta 55*ROW_STRIDE + rowBlit,x
    jsr e_56tooro
    jsr e_60otooo
    lsr
    sta 65*ROW_STRIDE + rowBlit,x
    sta 66*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_67oo
    jsr e_r70otooo
    jsr e_r75otooo
    lsr
    jsr e_80otooo
    jsr e_r85ootoo
    jsr e_r90ootoo
    jsr e_r95ootoo
    jsr e_r100ootoo
    jsr e_105rootoo
    jsr e_110rootoo
    jsr e_115rootoo
    jsr e_120rootoo
    jmp e_125roo

; Produce 168 rows from 64 rows
expand_168:
    jsr selectMip0
    ldy #3
    jsr e_t0ooroo
    jsr e_5tooroo
    jsr e_10toooro
    sta 15*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 16*ROW_STRIDE + rowBlit,x
    jsr e_17oro
    jsr e_20toooro
    jsr e_25otooro
    jsr e_30otooo
    jsr e_r35otooo
    lsr
    jsr e_40otooo
    jsr e_r45ootoo
    jsr e_r50ootoo
    jsr e_55rootoo
    jsr e_60rootoo
    jsr e_65roooto
    jsr e_70roooto
    jsr e_75orooto
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    lsr
    sta 82*ROW_STRIDE + rowBlit,x
    jsr e_83to
    jsr e_85orooo
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_92oo
    jsr e_t95ooroo
    jsr e_t100ooroo
    jsr e_t105ooroo
    jsr e_110tooroo
    jsr e_115toooro
    jsr e_120toooro
    jmp e_125too

; Produce 172 rows from 64 rows
expand_172:
    jsr selectMip0
    ldy #4
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10toooro
    jsr e_15otooro
    jsr e_20otooo
    jsr e_r25otooo
    jsr e_r30ootoo
    jsr e_r35ootoo
    jsr e_40rootoo
    jsr e_45roooto
    jsr e_50orooto
    jsr e_55orooo
    lda (pTex),y
    iny
    sta 60*ROW_STRIDE + rowBlit,x
    sta 61*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_62oo
    jsr e_t65ooroo
    jsr e_t70ooroo
    jsr e_75tooroo
    jsr e_80toooro
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    sta 87*ROW_STRIDE + rowBlit,x
    jsr e_88ro
    jsr e_90otooo
    lsr
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_97oo
    jsr e_r100ootoo
    jsr e_r105ootoo
    jsr e_110rootoo
    jsr e_115roooto
    jsr e_120roooto
    jmp e_125oro

; Produce 176 rows from 64 rows
expand_176:
    jsr selectMip0
    ldy #4
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10toooro
    jsr e_15otooo
    jsr e_r20otooo
    jsr e_r25ootoo
    jsr e_30rootoo
    jsr e_35roooto
    jsr e_40orooto
    jsr e_45orooo
    jsr e_t50ooroo
    jsr e_t55ooroo
    jsr e_60toooro
    jsr e_65toooro
    jsr e_70otooo
    jsr e_r75otooo
    jsr e_r80ootoo
    jsr e_85rootoo
    jsr e_90roooto
    sta 95*ROW_STRIDE + rowBlit,x
    sta 96*ROW_STRIDE + rowBlit,x
    lsr
    sta 97*ROW_STRIDE + rowBlit,x
    jsr e_98to
    jsr e_100orooo
    jsr e_t105ooroo
    jsr e_t110ooroo
    jsr e_115toooro
    jsr e_120toooro
    jmp e_125oto

; Produce 180 rows from 64 rows
expand_180:
    jsr selectMip0
    ldy #4
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_r15otooo
    lsr
    sta 20*ROW_STRIDE + rowBlit,x
    jsr e_21otoo
    jsr e_25roooto
    jsr e_30roooto
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_37oo
    jsr e_t40ooroo
    jsr e_t45ooroo
    jsr e_50toooro
    jsr e_55otooo
    jsr e_r60otooo
    jsr e_r65ootoo
    jsr e_70roooto
    sta 75*ROW_STRIDE + rowBlit,x
    lsr
    sta 76*ROW_STRIDE + rowBlit,x
    sta 77*ROW_STRIDE + rowBlit,x
    jsr e_78to
    jsr e_80orooo
    jsr e_t85ooroo
    jsr e_t90ooroo
    jsr e_95toooro
    jsr e_100otooo
    lsr
    jsr e_105otooo
    jsr e_r110ootoo
    jsr e_115roooto
    jsr e_120roooto
    jmp e_125oro

; Produce 184 rows from 64 rows
expand_184:
    jsr selectMip0
    ldy #4
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_r15ootoo
    jsr e_20rootoo
    jsr e_25roooto
    jsr e_30orooo
    jsr e_t35ooroo
    jsr e_40toooro
    jsr e_45toooro
    jsr e_50otooo
    jsr e_r55ootoo
    jsr e_60roooto
    jsr e_65orooto
    jsr e_70orooo
    jsr e_t75ooroo
    jsr e_80toooro
    jsr e_85otooo
    jsr e_r90otooo
    jsr e_r95ootoo
    jsr e_100roooto
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_107oo
    jsr e_t110ooroo
    jsr e_t115ooroo
    jsr e_120toooro
    jmp e_125oto

; Produce 188 rows from 64 rows
expand_188:
    jsr selectMip0
    ldy #5
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_r15ootoo
    jsr e_20roooto
    jsr e_25orooo
    jsr e_t30ooroo
    jsr e_35toooro
    jsr e_40otooo
    lsr
    jsr e_45otooo
    jsr e_r50ootoo
    jsr e_55roooto
    jsr e_60orooo
    jsr e_t65ooroo
    sta 70*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 71*ROW_STRIDE + rowBlit,x
    jsr e_72oro
    jsr e_75otooo
    jsr e_r80ootoo
    sta 85*ROW_STRIDE + rowBlit,x
    lsr
    sta 86*ROW_STRIDE + rowBlit,x
    jsr e_87oto
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    sta 92*ROW_STRIDE + rowBlit,x
    jsr e_93to
    jsr e_95orooo
    jsr e_t100ooroo
    sta 105*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 106*ROW_STRIDE + rowBlit,x
    jsr e_107oro
    jsr e_110otooo
    lsr
    sta 115*ROW_STRIDE + rowBlit,x
    jsr e_116otoo
    jsr e_120roooto
    jmp e_125oro

; Produce 192 rows from 64 rows
expand_192:
    jsr selectMip0
    ldy #5
    jsr e_t0ooroo
    jsr e_5toooro
    jsr e_10otooo
    jsr e_r15ootoo
    jsr e_20roooto
    jsr e_25orooo
    jsr e_t30ooroo
    jsr e_35toooro
    jsr e_40otooo
    jsr e_r45ootoo
    jsr e_50roooto
    jsr e_55orooo
    jsr e_t60ooroo
    jsr e_65toooro
    jsr e_70otooo
    jsr e_r75ootoo
    jsr e_80roooto
    jsr e_85orooo
    jsr e_t90ooroo
    jsr e_95toooro
    jsr e_100otooo
    jsr e_r105ootoo
    jsr e_110roooto
    jsr e_115orooo
    jsr e_t120ooroo
    jmp e_125too

; Produce 200 rows from 64 rows
expand_200:
    jsr selectMip0
    ldy #5
    jsr e_t0oooro
    jsr e_5otooo
    jsr e_r10ootoo
    sta 15*ROW_STRIDE + rowBlit,x
    lsr
    sta 16*ROW_STRIDE + rowBlit,x
    jsr e_17oto
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_22oo
    lda (pTex),y
    iny
    sta 25*ROW_STRIDE + rowBlit,x
    sta 26*ROW_STRIDE + rowBlit,x
    jsr e_27oro
    jsr e_30otooo
    jsr e_r35ootoo
    sta 40*ROW_STRIDE + rowBlit,x
    lsr
    sta 41*ROW_STRIDE + rowBlit,x
    sta 42*ROW_STRIDE + rowBlit,x
    jsr e_43to
    jsr e_45orooo
    lda (pTex),y
    iny
    sta 50*ROW_STRIDE + rowBlit,x
    sta 51*ROW_STRIDE + rowBlit,x
    jsr e_52oro
    jsr e_55otooo
    jsr e_r60ootoo
    jsr e_65roooto
    jsr e_70orooo
    jsr e_t75oooro
    jsr e_80otooo
    jsr e_r85ootoo
    jsr e_90roooto
    jsr e_95orooo
    lda (pTex),y
    iny
    sta 100*ROW_STRIDE + rowBlit,x
    sta 101*ROW_STRIDE + rowBlit,x
    jsr e_102oro
    jsr e_105otooo
    jsr e_r110ootoo
    jsr e_115roooto
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_122oo
    jmp e_t125oo

; Produce 208 rows from 64 rows
expand_208:
    jsr selectMip0
    ldy #6
    jsr e_t0oooro
    jsr e_5otooo
    jsr e_r10ootoo
    jsr e_15orooo
    jsr e_t20ooroo
    sta 25*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 26*ROW_STRIDE + rowBlit,x
    jsr e_27oo
    jsr e_r30ootoo
    jsr e_35roooto
    jsr e_40ooroo
    jsr e_45toooro
    jsr e_50otooo
    jsr e_55roooto
    jsr e_60orooo
    lda (pTex),y
    iny
    sta 65*ROW_STRIDE + rowBlit,x
    sta 66*ROW_STRIDE + rowBlit,x
    jsr e_67oro
    jsr e_70otooo
    jsr e_r75ootoo
    jsr e_80orooo
    jsr e_t85ooroo
    jsr e_90toooo
    jsr e_r95ootoo
    jsr e_100roooto
    jsr e_105ooroo
    jsr e_110toooro
    jsr e_115otooo
    jsr e_120roooto
    jmp e_125oro

; Produce 216 rows from 64 rows
expand_216:
    jsr selectMip0
    ldy #6
    jsr e_t0oooro
    jsr e_5otooo
    jsr e_10roooto
    jsr e_15orooo
    jsr e_20toooro
    jsr e_25otooo
    jsr e_30roooto
    sta 35*ROW_STRIDE + rowBlit,x
    jsr e_36oroo
    jsr e_40toooro
    sta 45*ROW_STRIDE + rowBlit,x
    jsr e_46otoo
    jsr e_50roooto
    jsr e_55ooroo
    sta 60*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 61*ROW_STRIDE + rowBlit,x
    jsr e_62oo
    jsr e_r65ootoo
    sta 70*ROW_STRIDE + rowBlit,x
    lsr
    sta 71*ROW_STRIDE + rowBlit,x
    jsr e_72oo
    jsr e_t75ooroo
    sta 80*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 81*ROW_STRIDE + rowBlit,x
    jsr e_82oo
    jsr e_r85ootoo
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_92oo
    jsr e_t95ooroo
    jsr e_100otooo
    jsr e_r105ootoo
    jsr e_110orooo
    lda (pTex),y
    iny
    sta 115*ROW_STRIDE + rowBlit,x
    jsr e_116ooro
    jsr e_120otooo
    lsr
    jsr e_125oo
    rts

; Produce 224 rows from 64 rows
expand_224:
    jsr selectMip0
    ldy #6
    jsr e_t0oooro
    jsr e_5otooo
    jsr e_10roooto
    sta 15*ROW_STRIDE + rowBlit,x
    jsr e_16oroo
    sta 20*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 21*ROW_STRIDE + rowBlit,x
    jsr e_22oo
    jsr e_r25ootoo
    jsr e_30orooo
    lda (pTex),y
    iny
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    jsr e_37oro
    jsr e_40otooo
    jsr e_45roooto
    jsr e_50ooroo
    sta 55*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 56*ROW_STRIDE + rowBlit,x
    jsr e_57oo
    jsr e_r60ootoo
    jsr e_65orooo
    jsr e_t70oooro
    jsr e_75otooo
    jsr e_80roooto
    jsr e_85ooroo
    jsr e_90toooo
    jsr e_r95ootoo
    jsr e_100orooo
    jsr e_t105oooro
    jsr e_110otooo
    jsr e_115roooto
    jsr e_120ooroo
    jmp e_125too

; Produce 232 rows from 64 rows
expand_232:
    jsr selectMip0
    ldy #7
    jsr e_t0oooro
    jsr e_5ootoo
    sta 10*ROW_STRIDE + rowBlit,x
    lsr
    sta 11*ROW_STRIDE + rowBlit,x
    jsr e_12oo
    jsr e_t15oooro
    jsr e_20otooo
    jsr e_25roooto
    sta 30*ROW_STRIDE + rowBlit,x
    jsr e_31oroo
    jsr e_35otooo
    lsr
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    sta 42*ROW_STRIDE + rowBlit,x
    jsr e_43to
    sta 45*ROW_STRIDE + rowBlit,x
    jsr e_46oroo
    sta 50*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    jsr e_51ooo
    jsr e_r55ootoo
    jsr e_60orooo
    jsr e_65toooro
    sta 70*ROW_STRIDE + rowBlit,x
    sta 71*ROW_STRIDE + rowBlit,x
    jsr e_72too
    sta 75*ROW_STRIDE + rowBlit,x
    sta 76*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_77oo
    lda (pTex),y
    iny
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    jsr e_82oro
    jsr e_85otooo
    sta 90*ROW_STRIDE + rowBlit,x
    lsr
    sta 91*ROW_STRIDE + rowBlit,x
    jsr e_92oo
    jsr e_t95ooroo
    jsr e_100otooo
    sta 105*ROW_STRIDE + rowBlit,x
    lsr
    sta 106*ROW_STRIDE + rowBlit,x
    sta 107*ROW_STRIDE + rowBlit,x
    jsr e_108to
    jsr e_110ooroo
    sta 115*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 116*ROW_STRIDE + rowBlit,x
    jsr e_117oo
    lsr
    sta 120*ROW_STRIDE + rowBlit,x
    sta 121*ROW_STRIDE + rowBlit,x
    jsr e_122oto
    jmp e_125oro

; Produce 240 rows from 64 rows
expand_240:
    jsr selectMip0
    ldy #7
    jsr e_t0oooro
    jsr e_5ootoo
    jsr e_10orooo
    jsr e_t15oooro
    sta 20*ROW_STRIDE + rowBlit,x
    jsr e_21otoo
    jsr e_25orooo
    lda (pTex),y
    iny
    sta 30*ROW_STRIDE + rowBlit,x
    sta 31*ROW_STRIDE + rowBlit,x
    jsr e_32oro
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    jsr e_37too
    sta 40*ROW_STRIDE + rowBlit,x
    sta 41*ROW_STRIDE + rowBlit,x
    lsr
    jsr e_42oo
    lda (pTex),y
    iny
    sta 45*ROW_STRIDE + rowBlit,x
    sta 46*ROW_STRIDE + rowBlit,x
    jsr e_47oro
    jsr e_50ootoo
    jsr e_55orooo
    lda (pTex),y
    iny
    sta 60*ROW_STRIDE + rowBlit,x
    jsr e_61ooro
    jsr e_65ootoo
    jsr e_70orooo
    jsr e_t75oooro
    sta 80*ROW_STRIDE + rowBlit,x
    sta 81*ROW_STRIDE + rowBlit,x
    jsr e_82too
    jsr e_85orooo
    lda (pTex),y
    iny
    sta 90*ROW_STRIDE + rowBlit,x
    sta 91*ROW_STRIDE + rowBlit,x
    sta 92*ROW_STRIDE + rowBlit,x
    jsr e_93ro
    jsr e_95ootoo
    jsr e_100orooo
    jsr e_t105oooro
    jsr e_110ootoo
    jsr e_115orooo
    lda (pTex),y
    iny
    sta 120*ROW_STRIDE + rowBlit,x
    jsr e_121ooro
    jmp e_125oo

; Produce 248 rows from 64 rows
expand_248:
    jsr selectMip0
    ldy #7
    jsr e_t0oooro
    jsr e_5ootoo
    jsr e_10orooo
    sta 15*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 16*ROW_STRIDE + rowBlit,x
    jsr e_17oo
    lsr
    sta 20*ROW_STRIDE + rowBlit,x
    sta 21*ROW_STRIDE + rowBlit,x
    jsr e_22oto
    jsr e_25ooroo
    sta 30*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 31*ROW_STRIDE + rowBlit,x
    jsr e_32oo
    lsr
    sta 35*ROW_STRIDE + rowBlit,x
    sta 36*ROW_STRIDE + rowBlit,x
    jsr e_37oto
    jsr e_40ooroo
    jsr e_45otooo
    jsr e_50roooo
    lda (pTex),y
    iny
    sta 55*ROW_STRIDE + rowBlit,x
    sta 56*ROW_STRIDE + rowBlit,x
    jsr e_57oro
    jsr e_60otooo
    sta 65*ROW_STRIDE + rowBlit,x
    lsr
    sta 66*ROW_STRIDE + rowBlit,x
    jsr e_67oo
    jsr e_t70oooro
    sta 75*ROW_STRIDE + rowBlit,x
    jsr e_76otoo
    jsr e_80orooo
    sta 85*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 86*ROW_STRIDE + rowBlit,x
    jsr e_87oo
    jsr e_r90ootoo
    jsr e_95orooo
    sta 100*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 101*ROW_STRIDE + rowBlit,x
    jsr e_102oo
    lsr
    sta 105*ROW_STRIDE + rowBlit,x
    sta 106*ROW_STRIDE + rowBlit,x
    sta 107*ROW_STRIDE + rowBlit,x
    jsr e_108to
    jsr e_110ooroo
    jsr e_115otooo
    jsr e_120roooto
    jmp e_125oo

e_125oro:
    sta 125*ROW_STRIDE + rowBlit,x
    sta 126*ROW_STRIDE + rowBlit,x
    lsr
    sta 127*ROW_STRIDE + rowBlit,x
    rts

e_125roo:
    sta 125*ROW_STRIDE + rowBlit,x
    lsr
    sta 126*ROW_STRIDE + rowBlit,x
    sta 127*ROW_STRIDE + rowBlit,x
    rts

e_125too:
    sta 125*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 126*ROW_STRIDE + rowBlit,x
    sta 127*ROW_STRIDE + rowBlit,x
    rts

e_120rootoo:
    sta 120*ROW_STRIDE + rowBlit,x
    lsr
    sta 121*ROW_STRIDE + rowBlit,x
    sta 122*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 123*ROW_STRIDE + rowBlit,x
    sta 124*ROW_STRIDE + rowBlit,x
    rts

e_t5orooto:
    lda (pTex),y
    iny
    sta 5*ROW_STRIDE + rowBlit,x
    sta 6*ROW_STRIDE + rowBlit,x
    lsr
    sta 7*ROW_STRIDE + rowBlit,x
    sta 8*ROW_STRIDE + rowBlit,x
    lda (pTex),y
    iny
    sta 9*ROW_STRIDE + rowBlit,x
    rts


genCFmeansRF_LU_fault_binerrs <- function() {

results <- data.frame(row.names=seq(1, 10))

LU_fault_binerrs_N_1_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_1=LU_fault_binerrs_all$N_1)
results[["N_1"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_1_treat_df, "Y", "N_1")

LU_fault_binerrs_minMN_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, minMN_0=LU_fault_binerrs_all$minMN_0, M_5=LU_fault_binerrs_all$M_5, N_10=LU_fault_binerrs_all$N_10)
results[["minMN_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_minMN_0_treat_df, "Y", "minMN_0")

LU_fault_binerrs_j_20_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_20=LU_fault_binerrs_all$j_20, j_19=LU_fault_binerrs_all$j_19)
results[["j_20"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_20_treat_df, "Y", "j_20")

LU_fault_binerrs_N_3_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_3=LU_fault_binerrs_all$N_3)
results[["N_3"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_3_treat_df, "Y", "N_3")

LU_fault_binerrs_N_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_5=LU_fault_binerrs_all$N_5)
results[["N_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_5_treat_df, "Y", "N_5")

LU_fault_binerrs_N_7_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_7=LU_fault_binerrs_all$N_7)
results[["N_7"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_7_treat_df, "Y", "N_7")

LU_fault_binerrs_i_11_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_11=LU_fault_binerrs_all$i_11, i_10=LU_fault_binerrs_all$i_10)
results[["i_11"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_11_treat_df, "Y", "i_11")

LU_fault_binerrs_j_23_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_23=LU_fault_binerrs_all$j_23, j_22=LU_fault_binerrs_all$j_22)
results[["j_23"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_23_treat_df, "Y", "j_23")

LU_fault_binerrs_i_14_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_14=LU_fault_binerrs_all$i_14, i_13=LU_fault_binerrs_all$i_13)
results[["i_14"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_14_treat_df, "Y", "i_14")

LU_fault_binerrs_N_9_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_9=LU_fault_binerrs_all$N_9)
results[["N_9"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_9_treat_df, "Y", "N_9")

LU_fault_binerrs_remainder_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, remainder_0=LU_fault_binerrs_all$remainder_0, N_7=LU_fault_binerrs_all$N_7)
results[["remainder_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_remainder_0_treat_df, "Y", "remainder_0")

LU_fault_binerrs_i_18_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_18=LU_fault_binerrs_all$i_18, i_17=LU_fault_binerrs_all$i_17)
results[["i_18"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_18_treat_df, "Y", "i_18")

LU_fault_binerrs_afterCopy_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, afterCopy_0=LU_fault_binerrs_all$afterCopy_0, forCopy_0=LU_fault_binerrs_all$forCopy_0)
results[["afterCopy_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_afterCopy_0_treat_df, "Y", "afterCopy_0")

LU_fault_binerrs_pivot__null_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, pivot__null=LU_fault_binerrs_all$pivot__null, M_4=LU_fault_binerrs_all$M_4)
results[["pivot__null"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_pivot__null_treat_df, "Y", "pivot__null")

LU_fault_binerrs_N_10_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_10=LU_fault_binerrs_all$N_10)
results[["N_10"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_10_treat_df, "Y", "N_10")

LU_fault_binerrs_N_12_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, N_12=LU_fault_binerrs_all$N_12)
results[["N_12"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_N_12_treat_df, "Y", "N_12")

LU_fault_binerrs_j_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_2=LU_fault_binerrs_all$j_2, j_1=LU_fault_binerrs_all$j_1)
results[["j_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_2_treat_df, "Y", "j_2")

LU_fault_binerrs_forCopy_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, forCopy_0=LU_fault_binerrs_all$forCopy_0)
results[["forCopy_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_forCopy_0_treat_df, "Y", "forCopy_0")

LU_fault_binerrs_j_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_5=LU_fault_binerrs_all$j_5, j_4=LU_fault_binerrs_all$j_4)
results[["j_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_5_treat_df, "Y", "j_5")

LU_fault_binerrs_j_8_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_8=LU_fault_binerrs_all$j_8, j_8=LU_fault_binerrs_all$j_8)
results[["j_8"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_8_treat_df, "Y", "j_8")

LU_fault_binerrs_Nd_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, Nd_0=LU_fault_binerrs_all$Nd_0, N_0=LU_fault_binerrs_all$N_0)
results[["Nd_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_Nd_0_treat_df, "Y", "Nd_0")

LU_fault_binerrs_j_7_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_7=LU_fault_binerrs_all$j_7)
results[["j_7"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_7_treat_df, "Y", "j_7")

LU_fault_binerrs_t_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, t_0=LU_fault_binerrs_all$t_0, j_10=LU_fault_binerrs_all$j_10)
results[["t_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_t_0_treat_df, "Y", "t_0")

LU_fault_binerrs_j_11_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_11=LU_fault_binerrs_all$j_11, j_10=LU_fault_binerrs_all$j_10)
results[["j_11"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_11_treat_df, "Y", "j_11")

LU_fault_binerrs_t_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, t_2=LU_fault_binerrs_all$t_2, t_0=LU_fault_binerrs_all$t_0, t_1=LU_fault_binerrs_all$t_1)
results[["t_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_t_2_treat_df, "Y", "t_2")

LU_fault_binerrs_t_1_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, t_1=LU_fault_binerrs_all$t_1, ab_0=LU_fault_binerrs_all$ab_0)
results[["t_1"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_t_1_treat_df, "Y", "t_1")

LU_fault_binerrs_t_3_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, t_3=LU_fault_binerrs_all$t_3, t_0=LU_fault_binerrs_all$t_0, t_2=LU_fault_binerrs_all$t_2)
results[["t_3"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_t_3_treat_df, "Y", "t_3")

LU_fault_binerrs_ip_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ip_0=LU_fault_binerrs_all$ip_0, i_16=LU_fault_binerrs_all$i_16)
results[["ip_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ip_0_treat_df, "Y", "ip_0")

LU_fault_binerrs_j_14_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_14=LU_fault_binerrs_all$j_14, j_11=LU_fault_binerrs_all$j_11, j_14=LU_fault_binerrs_all$j_14)
results[["j_14"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_14_treat_df, "Y", "j_14")

LU_fault_binerrs_j_17_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, j_17=LU_fault_binerrs_all$j_17, j_16=LU_fault_binerrs_all$j_16)
results[["j_17"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_j_17_treat_df, "Y", "j_17")

LU_fault_binerrs_temp_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, temp_0=LU_fault_binerrs_all$temp_0)
results[["temp_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_temp_0_treat_df, "Y", "temp_0")

LU_fault_binerrs_sum_3_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_3=LU_fault_binerrs_all$sum_3, sum_2=LU_fault_binerrs_all$sum_2, sum_0=LU_fault_binerrs_all$sum_0)
results[["sum_3"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_3_treat_df, "Y", "sum_3")

LU_fault_binerrs_sum_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_2=LU_fault_binerrs_all$sum_2, sum_1=LU_fault_binerrs_all$sum_1, sum_0=LU_fault_binerrs_all$sum_0)
results[["sum_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_2_treat_df, "Y", "sum_2")

LU_fault_binerrs_sum_1_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_1=LU_fault_binerrs_all$sum_1, j_13=LU_fault_binerrs_all$j_13, i_17=LU_fault_binerrs_all$i_17)
results[["sum_1"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_1_treat_df, "Y", "sum_1")

LU_fault_binerrs_sum_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_0=LU_fault_binerrs_all$sum_0, ip_0=LU_fault_binerrs_all$ip_0)
results[["sum_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_0_treat_df, "Y", "sum_0")

LU_fault_binerrs_M_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, M_0=LU_fault_binerrs_all$M_0)
results[["M_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_M_0_treat_df, "Y", "M_0")

LU_fault_binerrs_ab_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ab_0=LU_fault_binerrs_all$ab_0, j_10=LU_fault_binerrs_all$j_10, i_13=LU_fault_binerrs_all$i_13)
results[["ab_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ab_0_treat_df, "Y", "ab_0")

LU_fault_binerrs_getlu_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, getlu_0=LU_fault_binerrs_all$getlu_0)
results[["getlu_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_getlu_0_treat_df, "Y", "getlu_0")

LU_fault_binerrs_M_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, M_2=LU_fault_binerrs_all$M_2)
results[["M_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_M_2_treat_df, "Y", "M_2")

LU_fault_binerrs_M_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, M_5=LU_fault_binerrs_all$M_5)
results[["M_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_M_5_treat_df, "Y", "M_5")

LU_fault_binerrs_jj_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, jj_2=LU_fault_binerrs_all$jj_2, jj_1=LU_fault_binerrs_all$jj_1)
results[["jj_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_jj_2_treat_df, "Y", "jj_2")

LU_fault_binerrs_M_4_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, M_4=LU_fault_binerrs_all$M_4)
results[["M_4"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_M_4_treat_df, "Y", "M_4")

LU_fault_binerrs_jp_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, jp_0=LU_fault_binerrs_all$jp_0, j_10=LU_fault_binerrs_all$j_10)
results[["jp_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_jp_0_treat_df, "Y", "jp_0")

LU_fault_binerrs_M_9_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, M_9=LU_fault_binerrs_all$M_9)
results[["M_9"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_M_9_treat_df, "Y", "M_9")

LU_fault_binerrs_jp_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, jp_2=LU_fault_binerrs_all$jp_2, jp_0=LU_fault_binerrs_all$jp_0, jp_1=LU_fault_binerrs_all$jp_1)
results[["jp_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_jp_2_treat_df, "Y", "jp_2")

LU_fault_binerrs_lu_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, lu_0=LU_fault_binerrs_all$lu_0, temp_0=LU_fault_binerrs_all$temp_0)
results[["lu_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_lu_0_treat_df, "Y", "lu_0")

LU_fault_binerrs_jp_1_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, jp_1=LU_fault_binerrs_all$jp_1, i_13=LU_fault_binerrs_all$i_13)
results[["jp_1"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_jp_1_treat_df, "Y", "jp_1")

LU_fault_binerrs_sum_6_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_6=LU_fault_binerrs_all$sum_6, sum_5=LU_fault_binerrs_all$sum_5, sum_4=LU_fault_binerrs_all$sum_4)
results[["sum_6"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_6_treat_df, "Y", "sum_6")

LU_fault_binerrs_sum_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_5=LU_fault_binerrs_all$sum_5, i_20=LU_fault_binerrs_all$i_20, j_16=LU_fault_binerrs_all$j_16)
results[["sum_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_5_treat_df, "Y", "sum_5")

LU_fault_binerrs_jp_3_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, jp_3=LU_fault_binerrs_all$jp_3, jp_0=LU_fault_binerrs_all$jp_0, jp_2=LU_fault_binerrs_all$jp_2)
results[["jp_3"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_jp_3_treat_df, "Y", "jp_3")

LU_fault_binerrs_sum_4_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, sum_4=LU_fault_binerrs_all$sum_4, i_20=LU_fault_binerrs_all$i_20)
results[["sum_4"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_sum_4_treat_df, "Y", "sum_4")

LU_fault_binerrs_recp_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, recp_0=LU_fault_binerrs_all$recp_0, j_10=LU_fault_binerrs_all$j_10)
results[["recp_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_recp_0_treat_df, "Y", "recp_0")

LU_fault_binerrs_AiiJ_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, AiiJ_0=LU_fault_binerrs_all$AiiJ_0, j_10=LU_fault_binerrs_all$j_10)
results[["AiiJ_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_AiiJ_0_treat_df, "Y", "AiiJ_0")

LU_fault_binerrs_i_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_2=LU_fault_binerrs_all$i_2, i_1=LU_fault_binerrs_all$i_1)
results[["i_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_2_treat_df, "Y", "i_2")

LU_fault_binerrs_i_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_5=LU_fault_binerrs_all$i_5, i_4=LU_fault_binerrs_all$i_4)
results[["i_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_5_treat_df, "Y", "i_5")

LU_fault_binerrs_k_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, k_2=LU_fault_binerrs_all$k_2, k_1=LU_fault_binerrs_all$k_1)
results[["k_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_k_2_treat_df, "Y", "k_2")

LU_fault_binerrs_ii_2_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_2=LU_fault_binerrs_all$ii_2, ii_1=LU_fault_binerrs_all$ii_1)
results[["ii_2"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_2_treat_df, "Y", "ii_2")

LU_fault_binerrs_i_8_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_8=LU_fault_binerrs_all$i_8, i_7=LU_fault_binerrs_all$i_7)
results[["i_8"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_8_treat_df, "Y", "i_8")

LU_fault_binerrs_ii_4_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_4=LU_fault_binerrs_all$ii_4, i_17=LU_fault_binerrs_all$i_17)
results[["ii_4"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_4_treat_df, "Y", "ii_4")

LU_fault_binerrs_i_21_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_21=LU_fault_binerrs_all$i_21, i_20=LU_fault_binerrs_all$i_20)
results[["i_21"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_21_treat_df, "Y", "i_21")

LU_fault_binerrs_LU__null_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, LU__null=LU_fault_binerrs_all$LU__null, M_4=LU_fault_binerrs_all$M_4, N_9=LU_fault_binerrs_all$N_9)
results[["LU__null"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_LU__null_treat_df, "Y", "LU__null")

LU_fault_binerrs_ii_3_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_3=LU_fault_binerrs_all$ii_3)
results[["ii_3"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_3_treat_df, "Y", "ii_3")

LU_fault_binerrs_ii_6_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_6=LU_fault_binerrs_all$ii_6, ii_3=LU_fault_binerrs_all$ii_3, ii_5=LU_fault_binerrs_all$ii_5)
results[["ii_6"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_6_treat_df, "Y", "ii_6")

LU_fault_binerrs_ii_5_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_5=LU_fault_binerrs_all$ii_5, ii_4=LU_fault_binerrs_all$ii_4, ii_3=LU_fault_binerrs_all$ii_3)
results[["ii_5"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_5_treat_df, "Y", "ii_5")

LU_fault_binerrs_ii_7_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, ii_7=LU_fault_binerrs_all$ii_7, ii_3=LU_fault_binerrs_all$ii_3, ii_6=LU_fault_binerrs_all$ii_6)
results[["ii_7"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_ii_7_treat_df, "Y", "ii_7")

LU_fault_binerrs_i_24_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_24=LU_fault_binerrs_all$i_24, i_23=LU_fault_binerrs_all$i_23)
results[["i_24"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_24_treat_df, "Y", "i_24")

LU_fault_binerrs_i_27_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, i_27=LU_fault_binerrs_all$i_27, i_26=LU_fault_binerrs_all$i_26)
results[["i_27"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_i_27_treat_df, "Y", "i_27")

LU_fault_binerrs_pivot_0_treat_df <- data.frame(Y=LU_fault_binerrs_all$Y, pivot_0=LU_fault_binerrs_all$pivot_0)
results[["pivot_0"]] <- CFmeansForDecileBinsRF(LU_fault_binerrs_pivot_0_treat_df, "Y", "pivot_0")

return(results)

}

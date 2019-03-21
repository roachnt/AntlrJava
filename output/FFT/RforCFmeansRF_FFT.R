genCFmeansRF_FFT_fault_binerrs <- function() {

results <- data.frame(row.names=seq(1, 10))

FFT_fault_binerrs_i_12_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_12=FFT_fault_binerrs_all$i_12, b_1=FFT_fault_binerrs_all$b_1)
results[["i_12"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_12_treat_df, "Y", "i_12")

FFT_fault_binerrs_i_11_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_11=FFT_fault_binerrs_all$i_11, i_10=FFT_fault_binerrs_all$i_10)
results[["i_11"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_11_treat_df, "Y", "i_11")

FFT_fault_binerrs_i_14_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_14=FFT_fault_binerrs_all$i_14)
results[["i_14"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_14_treat_df, "Y", "i_14")

FFT_fault_binerrs_i_13_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_13=FFT_fault_binerrs_all$i_13, a_1=FFT_fault_binerrs_all$a_1, b_4=FFT_fault_binerrs_all$b_4)
results[["i_13"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_13_treat_df, "Y", "i_13")

FFT_fault_binerrs_i_16_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_16=FFT_fault_binerrs_all$i_16, i_15=FFT_fault_binerrs_all$i_15)
results[["i_16"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_16_treat_df, "Y", "i_16")

FFT_fault_binerrs_d_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, d_0=FFT_fault_binerrs_all$d_0, i_4=FFT_fault_binerrs_all$i_4)
results[["d_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_d_0_treat_df, "Y", "d_0")

FFT_fault_binerrs_b_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, b_2=FFT_fault_binerrs_all$b_2, b_2=FFT_fault_binerrs_all$b_2)
results[["b_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_b_2_treat_df, "Y", "b_2")

FFT_fault_binerrs_b_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, b_1=FFT_fault_binerrs_all$b_1, dual_0=FFT_fault_binerrs_all$dual_0)
results[["b_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_b_1_treat_df, "Y", "b_1")

FFT_fault_binerrs_b_4_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, b_4=FFT_fault_binerrs_all$b_4, dual_1=FFT_fault_binerrs_all$dual_1)
results[["b_4"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_b_4_treat_df, "Y", "b_4")

FFT_fault_binerrs_bit_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, bit_2=FFT_fault_binerrs_all$bit_2, bit_1=FFT_fault_binerrs_all$bit_1)
results[["bit_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_bit_2_treat_df, "Y", "bit_2")

FFT_fault_binerrs_b_5_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, b_5=FFT_fault_binerrs_all$b_5, b_5=FFT_fault_binerrs_all$b_5)
results[["b_5"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_b_5_treat_df, "Y", "b_5")

FFT_fault_binerrs_j_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_0=FFT_fault_binerrs_all$j_0, b_1=FFT_fault_binerrs_all$b_1, dual_0=FFT_fault_binerrs_all$dual_0)
results[["j_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_0_treat_df, "Y", "j_0")

FFT_fault_binerrs_tmp_imag_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, tmp_imag_0=FFT_fault_binerrs_all$tmp_imag_0, w_imag_0=FFT_fault_binerrs_all$w_imag_0, s_0=FFT_fault_binerrs_all$s_0, s2_0=FFT_fault_binerrs_all$s2_0, w_real_0=FFT_fault_binerrs_all$w_real_0)
results[["tmp_imag_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_tmp_imag_0_treat_df, "Y", "tmp_imag_0")

FFT_fault_binerrs_j_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_2=FFT_fault_binerrs_all$j_2)
results[["j_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_2_treat_df, "Y", "j_2")

FFT_fault_binerrs_tmp_imag_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, tmp_imag_1=FFT_fault_binerrs_all$tmp_imag_1, ii_0=FFT_fault_binerrs_all$ii_0)
results[["tmp_imag_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_tmp_imag_1_treat_df, "Y", "tmp_imag_1")

FFT_fault_binerrs_j_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_1=FFT_fault_binerrs_all$j_1, a_1=FFT_fault_binerrs_all$a_1, b_4=FFT_fault_binerrs_all$b_4, dual_1=FFT_fault_binerrs_all$dual_1)
results[["j_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_1_treat_df, "Y", "j_1")

FFT_fault_binerrs_n_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, n_0=FFT_fault_binerrs_all$n_0, nd_0=FFT_fault_binerrs_all$nd_0)
results[["n_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_n_0_treat_df, "Y", "n_0")

FFT_fault_binerrs_j_3_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_3=FFT_fault_binerrs_all$j_3, k_3=FFT_fault_binerrs_all$k_3)
results[["j_3"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_3_treat_df, "Y", "j_3")

FFT_fault_binerrs_n_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, n_2=FFT_fault_binerrs_all$n_2, i_10=FFT_fault_binerrs_all$i_10)
results[["n_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_n_2_treat_df, "Y", "n_2")

FFT_fault_binerrs_j_6_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_6=FFT_fault_binerrs_all$j_6, k_6=FFT_fault_binerrs_all$k_6)
results[["j_6"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_6_treat_df, "Y", "j_6")

FFT_fault_binerrs_n_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, n_1=FFT_fault_binerrs_all$n_1)
results[["n_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_n_1_treat_df, "Y", "n_1")

FFT_fault_binerrs_wd_imag_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, wd_imag_1=FFT_fault_binerrs_all$wd_imag_1, w_imag_1=FFT_fault_binerrs_all$w_imag_1, z1_imag_0=FFT_fault_binerrs_all$z1_imag_0, w_real_1=FFT_fault_binerrs_all$w_real_1, z1_real_0=FFT_fault_binerrs_all$z1_real_0)
results[["wd_imag_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_wd_imag_1_treat_df, "Y", "wd_imag_1")

FFT_fault_binerrs_j_5_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_5=FFT_fault_binerrs_all$j_5, j_2=FFT_fault_binerrs_all$j_2, j_4=FFT_fault_binerrs_all$j_4)
results[["j_5"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_5_treat_df, "Y", "j_5")

FFT_fault_binerrs_n_4_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, n_4=FFT_fault_binerrs_all$n_4)
results[["n_4"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_n_4_treat_df, "Y", "n_4")

FFT_fault_binerrs_wd_imag_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, wd_imag_0=FFT_fault_binerrs_all$wd_imag_0, j_0=FFT_fault_binerrs_all$j_0)
results[["wd_imag_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_wd_imag_0_treat_df, "Y", "wd_imag_0")

FFT_fault_binerrs_Nd_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, Nd_0=FFT_fault_binerrs_all$Nd_0, N_0=FFT_fault_binerrs_all$N_0)
results[["Nd_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_Nd_0_treat_df, "Y", "Nd_0")

FFT_fault_binerrs_j_7_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, j_7=FFT_fault_binerrs_all$j_7, j_6=FFT_fault_binerrs_all$j_6, j_2=FFT_fault_binerrs_all$j_2)
results[["j_7"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_j_7_treat_df, "Y", "j_7")

FFT_fault_binerrs_t_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, t_0=FFT_fault_binerrs_all$t_0, theta_0=FFT_fault_binerrs_all$theta_0)
results[["t_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_t_0_treat_df, "Y", "t_0")

FFT_fault_binerrs_s2_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, s2_0=FFT_fault_binerrs_all$s2_0, t_0=FFT_fault_binerrs_all$t_0)
results[["s2_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_s2_0_treat_df, "Y", "s2_0")

FFT_fault_binerrs_n_7_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, n_7=FFT_fault_binerrs_all$n_7)
results[["n_7"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_n_7_treat_df, "Y", "n_7")

FFT_fault_binerrs_logN_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, logN_0=FFT_fault_binerrs_all$logN_0, N_0=FFT_fault_binerrs_all$N_0)
results[["logN_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_logN_0_treat_df, "Y", "logN_0")

FFT_fault_binerrs_w_imag_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_imag_1=FFT_fault_binerrs_all$w_imag_1, tmp_imag_0=FFT_fault_binerrs_all$tmp_imag_0)
results[["w_imag_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_imag_1_treat_df, "Y", "w_imag_1")

FFT_fault_binerrs_w_imag_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_imag_2=FFT_fault_binerrs_all$w_imag_2, w_imag_1=FFT_fault_binerrs_all$w_imag_1, w_imag_0=FFT_fault_binerrs_all$w_imag_0)
results[["w_imag_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_imag_2_treat_df, "Y", "w_imag_2")

FFT_fault_binerrs_w_imag_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_imag_0=FFT_fault_binerrs_all$w_imag_0)
results[["w_imag_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_imag_0_treat_df, "Y", "w_imag_0")

FFT_fault_binerrs_jj_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, jj_0=FFT_fault_binerrs_all$jj_0, j_2=FFT_fault_binerrs_all$j_2)
results[["jj_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_jj_0_treat_df, "Y", "jj_0")

FFT_fault_binerrs_logn_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, logn_0=FFT_fault_binerrs_all$logn_0, n_4=FFT_fault_binerrs_all$n_4)
results[["logn_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_logn_0_treat_df, "Y", "logn_0")

FFT_fault_binerrs_z1_imag_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, z1_imag_0=FFT_fault_binerrs_all$z1_imag_0, j_1=FFT_fault_binerrs_all$j_1)
results[["z1_imag_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_z1_imag_0_treat_df, "Y", "z1_imag_0")

FFT_fault_binerrs_nm1_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, nm1_0=FFT_fault_binerrs_all$nm1_0, n_7=FFT_fault_binerrs_all$n_7)
results[["nm1_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_nm1_0_treat_df, "Y", "nm1_0")

FFT_fault_binerrs_log_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, log_0=FFT_fault_binerrs_all$log_0)
results[["log_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_log_0_treat_df, "Y", "log_0")

FFT_fault_binerrs_log_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, log_1=FFT_fault_binerrs_all$log_1, log_0=FFT_fault_binerrs_all$log_0)
results[["log_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_log_1_treat_df, "Y", "log_1")

FFT_fault_binerrs_nd_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, nd_0=FFT_fault_binerrs_all$nd_0)
results[["nd_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_nd_0_treat_df, "Y", "nd_0")

FFT_fault_binerrs_tmp_real_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, tmp_real_0=FFT_fault_binerrs_all$tmp_real_0, s_0=FFT_fault_binerrs_all$s_0, w_imag_0=FFT_fault_binerrs_all$w_imag_0, s2_0=FFT_fault_binerrs_all$s2_0, w_real_0=FFT_fault_binerrs_all$w_real_0)
results[["tmp_real_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_tmp_real_0_treat_df, "Y", "tmp_real_0")

FFT_fault_binerrs_a_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, a_2=FFT_fault_binerrs_all$a_2, a_1=FFT_fault_binerrs_all$a_1)
results[["a_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_a_2_treat_df, "Y", "a_2")

FFT_fault_binerrs_nd_4_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, nd_4=FFT_fault_binerrs_all$nd_4, n_0=FFT_fault_binerrs_all$n_0)
results[["nd_4"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_nd_4_treat_df, "Y", "nd_4")

FFT_fault_binerrs_nd_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, nd_2=FFT_fault_binerrs_all$nd_2)
results[["nd_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_nd_2_treat_df, "Y", "nd_2")

FFT_fault_binerrs_tmp_real_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, tmp_real_1=FFT_fault_binerrs_all$tmp_real_1, ii_0=FFT_fault_binerrs_all$ii_0)
results[["tmp_real_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_tmp_real_1_treat_df, "Y", "tmp_real_1")

FFT_fault_binerrs_k_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, k_1=FFT_fault_binerrs_all$k_1)
results[["k_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_k_1_treat_df, "Y", "k_1")

FFT_fault_binerrs_i_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_2=FFT_fault_binerrs_all$i_2, i_1=FFT_fault_binerrs_all$i_1)
results[["i_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_2_treat_df, "Y", "i_2")

FFT_fault_binerrs_i_5_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_5=FFT_fault_binerrs_all$i_5, i_4=FFT_fault_binerrs_all$i_4)
results[["i_5"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_5_treat_df, "Y", "i_5")

FFT_fault_binerrs_k_3_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, k_3=FFT_fault_binerrs_all$k_3, n_7=FFT_fault_binerrs_all$n_7)
results[["k_3"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_k_3_treat_df, "Y", "k_3")

FFT_fault_binerrs_k_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, k_2=FFT_fault_binerrs_all$k_2, k_2=FFT_fault_binerrs_all$k_2)
results[["k_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_k_2_treat_df, "Y", "k_2")

FFT_fault_binerrs_theta_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, theta_0=FFT_fault_binerrs_all$theta_0, direction_0=FFT_fault_binerrs_all$direction_0, dual_0=FFT_fault_binerrs_all$dual_0)
results[["theta_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_theta_0_treat_df, "Y", "theta_0")

FFT_fault_binerrs_ii_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, ii_0=FFT_fault_binerrs_all$ii_0, i_15=FFT_fault_binerrs_all$i_15)
results[["ii_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_ii_0_treat_df, "Y", "ii_0")

FFT_fault_binerrs_k_4_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, k_4=FFT_fault_binerrs_all$k_4)
results[["k_4"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_k_4_treat_df, "Y", "k_4")

FFT_fault_binerrs_i_8_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, i_8=FFT_fault_binerrs_all$i_8, i_7=FFT_fault_binerrs_all$i_7)
results[["i_8"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_i_8_treat_df, "Y", "i_8")

FFT_fault_binerrs_k_6_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, k_6=FFT_fault_binerrs_all$k_6, k_5=FFT_fault_binerrs_all$k_5, k_3=FFT_fault_binerrs_all$k_3)
results[["k_6"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_k_6_treat_df, "Y", "k_6")

FFT_fault_binerrs_diff_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, diff_2=FFT_fault_binerrs_all$diff_2, diff_0=FFT_fault_binerrs_all$diff_0, diff_1=FFT_fault_binerrs_all$diff_1)
results[["diff_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_diff_2_treat_df, "Y", "diff_2")

FFT_fault_binerrs_s_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, s_0=FFT_fault_binerrs_all$s_0, theta_0=FFT_fault_binerrs_all$theta_0)
results[["s_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_s_0_treat_df, "Y", "s_0")

FFT_fault_binerrs_diff_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, diff_0=FFT_fault_binerrs_all$diff_0)
results[["diff_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_diff_0_treat_df, "Y", "diff_0")

FFT_fault_binerrs_norm_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, norm_0=FFT_fault_binerrs_all$norm_0, n_0=FFT_fault_binerrs_all$n_0)
results[["norm_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_norm_0_treat_df, "Y", "norm_0")

FFT_fault_binerrs_diff_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, diff_1=FFT_fault_binerrs_all$diff_1, d_0=FFT_fault_binerrs_all$d_0)
results[["diff_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_diff_1_treat_df, "Y", "diff_1")

FFT_fault_binerrs_dual_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, dual_2=FFT_fault_binerrs_all$dual_2, dual_2=FFT_fault_binerrs_all$dual_2)
results[["dual_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_dual_2_treat_df, "Y", "dual_2")

FFT_fault_binerrs_dual_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, dual_0=FFT_fault_binerrs_all$dual_0)
results[["dual_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_dual_0_treat_df, "Y", "dual_0")

FFT_fault_binerrs_w_real_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_real_0=FFT_fault_binerrs_all$w_real_0)
results[["w_real_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_real_0_treat_df, "Y", "w_real_0")

FFT_fault_binerrs_wd_real_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, wd_real_1=FFT_fault_binerrs_all$wd_real_1, w_imag_1=FFT_fault_binerrs_all$w_imag_1, z1_imag_0=FFT_fault_binerrs_all$z1_imag_0, w_real_1=FFT_fault_binerrs_all$w_real_1, z1_real_0=FFT_fault_binerrs_all$z1_real_0)
results[["wd_real_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_wd_real_1_treat_df, "Y", "wd_real_1")

FFT_fault_binerrs_w_real_2_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_real_2=FFT_fault_binerrs_all$w_real_2, w_real_0=FFT_fault_binerrs_all$w_real_0, w_real_1=FFT_fault_binerrs_all$w_real_1)
results[["w_real_2"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_real_2_treat_df, "Y", "w_real_2")

FFT_fault_binerrs_w_real_1_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, w_real_1=FFT_fault_binerrs_all$w_real_1, tmp_real_0=FFT_fault_binerrs_all$tmp_real_0)
results[["w_real_1"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_w_real_1_treat_df, "Y", "w_real_1")

FFT_fault_binerrs_wd_real_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, wd_real_0=FFT_fault_binerrs_all$wd_real_0, j_0=FFT_fault_binerrs_all$j_0)
results[["wd_real_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_wd_real_0_treat_df, "Y", "wd_real_0")

FFT_fault_binerrs_z1_real_0_treat_df <- data.frame(Y=FFT_fault_binerrs_all$Y, z1_real_0=FFT_fault_binerrs_all$z1_real_0, j_1=FFT_fault_binerrs_all$j_1)
results[["z1_real_0"]] <- CFmeansForDecileBinsRF(FFT_fault_binerrs_z1_real_0_treat_df, "Y", "z1_real_0")

return(results)

}

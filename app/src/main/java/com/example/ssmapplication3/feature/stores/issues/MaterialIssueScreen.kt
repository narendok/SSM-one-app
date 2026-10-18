package com.example.ssmapplication3.feature.stores.issues

import androidx.compose.runtime.Composable
import com.example.ssmapplication3.feature.stores.StoresRepository
import com.example.ssmapplication3.feature.stores.StoresViewModel
import com.example.ssmapplication3.feature.stores.StoresScreen

class MaterialIssueViewModel(repository: StoresRepository) : StoresViewModel(repository, issue = true)
@Composable fun MaterialIssueScreen(viewModel: MaterialIssueViewModel) = StoresScreen(viewModel)

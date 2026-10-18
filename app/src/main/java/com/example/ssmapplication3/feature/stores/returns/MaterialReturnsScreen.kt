package com.example.ssmapplication3.feature.stores.returns

import androidx.compose.runtime.Composable
import com.example.ssmapplication3.feature.stores.StoresRepository
import com.example.ssmapplication3.feature.stores.StoresViewModel
import com.example.ssmapplication3.feature.stores.StoresScreen

class MaterialReturnsViewModel(repository: StoresRepository) : StoresViewModel(repository, issue = false)
@Composable fun MaterialReturnsScreen(viewModel: MaterialReturnsViewModel) = StoresScreen(viewModel)

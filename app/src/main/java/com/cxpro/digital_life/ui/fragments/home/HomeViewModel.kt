package com.cxpro.digital_life.ui.fragments.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class HomeViewModel(private val test: String = "This is home Fragment") : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = test
    }
    val text: LiveData<String> = _text

    class Factory(private val test: String = "This is home Fragment"):
        ViewModelProvider.Factory{
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(test) as T
        }
    }
}
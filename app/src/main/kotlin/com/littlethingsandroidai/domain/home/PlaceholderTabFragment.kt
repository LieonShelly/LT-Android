package com.littlethingsandroidai.domain.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.littlethingsandroidai.databinding.FragmentPlaceholderTabBinding

class PlaceholderTabFragment : Fragment() {
    private var _binding: FragmentPlaceholderTabBinding? = null
    private val binding: FragmentPlaceholderTabBinding
        get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPlaceholderTabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tabName = arguments?.getString(ARG_TAB_NAME).orEmpty()
        binding.tabNameText.text = tabName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TAB_NAME = "arg_tab_name"

        fun newInstance(tabName: String): PlaceholderTabFragment =
            PlaceholderTabFragment().apply {
                arguments =
                    Bundle().apply {
                        putString(ARG_TAB_NAME, tabName)
                    }
            }
    }
}

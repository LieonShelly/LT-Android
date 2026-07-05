package com.littlethingsandroidai.domain.calendar.detail

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil3.load
import coil3.request.crossfade
import coil3.request.error
import coil3.request.placeholder
import com.littlethingsandroidai.R
import com.littlethingsandroidai.databinding.FragmentReflectionDetailBinding
import com.littlethingsandroidai.domain.calendar.model.Answer

class ReflectionDetailFragment : Fragment(R.layout.fragment_reflection_detail) {
    private var _binding: FragmentReflectionDetailBinding? = null
    private val binding: FragmentReflectionDetailBinding
        get() = requireNotNull(_binding)

    private val viewModel: ReflectionDetailViewModel by viewModels {
        ReflectionDetailViewModelFactory(requireNotNull(requireArguments().toAnswer()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReflectionDetailBinding.bind(view)

        binding.detailBackButton.setOnClickListener {
            requireParentFragment().childFragmentManager.popBackStack()
        }

        bindContent(viewModel.answer)
    }

    private fun bindContent(answer: Answer) {
        binding.detailQuestionTitle.text = answer.question?.title.orEmpty()
        binding.detailAnswerContent.text = answer.content
        val categoryName = answer.question?.category?.name
        if (categoryName.isNullOrBlank()) {
            binding.detailCategory.visibility = View.GONE
        } else {
            binding.detailCategory.visibility = View.VISIBLE
            binding.detailCategory.text = categoryName
        }

        val icon = answer.icon
        if (icon?.status == ICON_STATUS_GENERATED && !icon.url.isNullOrBlank()) {
            binding.detailIcon.load(icon.url) {
                placeholder(R.drawable.ic_calendar_stamp_placeholder)
                error(R.drawable.ic_calendar_stamp_placeholder)
                crossfade(true)
            }
        } else {
            binding.detailIcon.setImageResource(R.drawable.ic_calendar_stamp_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ICON_STATUS_GENERATED = "GENERATED"
        private const val ARG_ANSWER_ID = "answer_id"
        private const val ARG_ANSWER_CONTENT = "answer_content"
        private const val ARG_QUESTION_TITLE = "question_title"
        private const val ARG_CATEGORY_NAME = "category_name"
        private const val ARG_ICON_ID = "icon_id"
        private const val ARG_ICON_URL = "icon_url"
        private const val ARG_ICON_STATUS = "icon_status"
        private const val ARG_ICON_READ_AT = "icon_read_at"

        fun newInstance(answer: Answer): ReflectionDetailFragment =
            ReflectionDetailFragment().apply {
                arguments = answer.toBundle()
            }

        private fun Answer.toBundle(): Bundle =
            bundleOf(
                ARG_ANSWER_ID to id,
                ARG_ANSWER_CONTENT to content,
                ARG_QUESTION_TITLE to question?.title,
                ARG_CATEGORY_NAME to question?.category?.name,
                ARG_ICON_ID to icon?.id,
                ARG_ICON_URL to icon?.url,
                ARG_ICON_STATUS to icon?.status,
                ARG_ICON_READ_AT to icon?.readAt,
            )

        private fun Bundle.toAnswer(): Answer? {
            val answerId = getString(ARG_ANSWER_ID) ?: return null
            val iconId = getString(ARG_ICON_ID)
            val iconStatus = getString(ARG_ICON_STATUS).orEmpty()
            return Answer(
                id = answerId,
                content = getString(ARG_ANSWER_CONTENT).orEmpty(),
                question =
                    com.littlethingsandroidai.domain.calendar.model.Question(
                        id = "",
                        title = getString(ARG_QUESTION_TITLE).orEmpty(),
                        category =
                            getString(ARG_CATEGORY_NAME)?.let { name ->
                                com.littlethingsandroidai.domain.calendar.model.Category(
                                    id = "",
                                    name = name,
                                )
                            },
                    ),
                icon =
                    if (iconId != null) {
                        com.littlethingsandroidai.domain.calendar.model.Icon(
                            id = iconId,
                            url = getString(ARG_ICON_URL),
                            status = iconStatus,
                            readAt = getString(ARG_ICON_READ_AT),
                        )
                    } else {
                        null
                    },
            )
        }
    }
}

package com.littlethingsandroidai.domain.signin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.littlethingsandroidai.R
import com.littlethingsandroidai.app.AppGraph
import com.littlethingsandroidai.app.home.HomeActivity
import com.littlethingsandroidai.databinding.FragmentSignInBinding
import kotlinx.coroutines.launch

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding: FragmentSignInBinding
        get() = requireNotNull(_binding)

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                return@registerForActivityResult
            }
            val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = signInTask.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    Toast.makeText(requireContext(), R.string.sign_in_google_token_empty, Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                viewModel.signInWithGoogle(idToken)
            } catch (apiException: ApiException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.sign_in_google_failed_code, apiException.statusCode),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }

    private val googleSignInClient by lazy(LazyThreadSafetyMode.NONE) {
        val options =
            GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build()
        GoogleSignIn.getClient(requireContext(), options)
    }

    private val viewModel: SignInViewModel by viewModels {
        SignInViewModel.Factory(
            appDataService = AppGraph.current.appDataWithAuthorizationService,
            onLoginSuccess = ::navigateToHome,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeViewModel()
    }

    private fun setupActions() {
        binding.googleSignInButton.setOnClickListener {
            if (!binding.termsCheckBox.isChecked) {
                val shake = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)
                binding.termsCheckBox.startAnimation(shake)
                Toast.makeText(requireContext(), R.string.sign_in_terms_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loading.collect { loading ->
                        binding.signInLoadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
                        binding.googleSignInButton.isEnabled = !loading
                        binding.termsCheckBox.isEnabled = !loading
                    }
                }
                launch {
                    viewModel.errorMessage.collect { error ->
                        if (error.isNullOrBlank()) {
                            return@collect
                        }
                        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun navigateToHome() {
        startActivity(Intent(requireContext(), HomeActivity::class.java))
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

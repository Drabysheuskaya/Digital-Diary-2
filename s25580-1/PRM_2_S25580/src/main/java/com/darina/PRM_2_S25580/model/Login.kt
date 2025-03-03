package com.darina.PRM_2_S25580.model


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val pin = binding.pinEditText.text.toString()
            if (pin.length < 4 || pin.length > 10) {
                Toast.makeText(this, R.string.pin_length_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val storedPin = pinViewModel.getPin()
                if (storedPin == null) {
                    if (isSettingPin) {
                        val confirmPin = binding.confirmPinEditText.text.toString()
                        if (pin == confirmPin) {
                            pinViewModel.setPin(SecurityUtils.hashPin(pin))
                            Toast.makeText(this@LoginActivity, R.string.pin_set, Toast.LENGTH_SHORT).show()
                            proceedToMainActivity()
                        } else {
                            Toast.makeText(this@LoginActivity, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        binding.confirmPinEditText.visibility = View.VISIBLE
                        isSettingPin = true
                        Toast.makeText(this@LoginActivity, R.string.pin_confirm, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (SecurityUtils.verifyPin(pin, storedPin.pinHash)) {
                        proceedToMainActivity()
                    } else {
                        Toast.makeText(this@LoginActivity, R.string.pin_incorrect, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


@Singleton
class NetworkUtils @Inject constructor(private val context: Context) {
    
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailableFlow: StateFlow<Boolean> = _isNetworkAvailable.asStateFlow()
    
    private val connectivityManager by lazy {
        context.getSystemService<ConnectivityManager>()
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isNetworkAvailable.value = true
        }
        
        override fun onLost(network: Network) {
            _isNetworkAvailable.value = false
        }
    }
    
    init {
        registerNetworkCallback()
        updateNetworkStatus()
    }
    
    private fun registerNetworkCallback() {
        connectivityManager?.let { cm ->
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(networkRequest, networkCallback)
        }
    }
    
    private fun updateNetworkStatus() {
        _isNetworkAvailable.value = isNetworkAvailable()
    }
    
    fun isNetworkAvailable(): Boolean {
        connectivityManager?.let { cm ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val networkCapabilities = cm.getNetworkCapabilities(cm.activeNetwork)
                return networkCapabilities != null && 
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = cm.activeNetworkInfo
                @Suppress("DEPRECATION")
                return activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        }
        return false
    }
    
    fun unregisterNetworkCallback() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Callback was not registered or already unregistered
        }
    }
}

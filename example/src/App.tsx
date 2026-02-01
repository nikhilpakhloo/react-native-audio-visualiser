import { useState, useEffect, useRef } from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Text,
  StatusBar,
  ScrollView,
  Alert,
  Platform,
  PermissionsAndroid,
} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';
import {
  AudioVisualizerView,
  AudioVisualizerModule,
  type VisualizationStyle,
} from 'react-native-audio-visualizer';

const VISUALIZATION_STYLES: { name: VisualizationStyle; label: string }[] = [
  { name: 'strings', label: 'Siri Strings' },
  { name: 'bars', label: 'DJ Bars' },
  { name: 'waves', label: 'Fluid Waves' },
  { name: 'thin-bars', label: 'Thin Lines' },
  { name: 'particles', label: 'Particles' },
  { name: 'orbit', label: 'Orbit Pulse' },
  { name: 'waveform', label: 'Waveform' },
];

const COLORS = [
  { value: '#00D9FF', label: 'Cyan' },
  { value: '#FF006E', label: 'Pink' },
  { value: '#8338EC', label: 'Purple' },
  { value: '#3A86FF', label: 'Blue' },
  { value: '#FFBE0B', label: 'Yellow' },
  { value: '#FB5607', label: 'Orange' },
  { value: '#06FFA5', label: 'Green' },
];

export default function App() {
  return (
    <SafeAreaProvider>
      <MainApp />
    </SafeAreaProvider>
  );
}

function MainApp() {
  const insets = useSafeAreaInsets();

  const [isRecording, setIsRecording] = useState(false);
  const [selectedStyle, setSelectedStyle] =
    useState<VisualizationStyle>('strings');
  const [selectedColor, setSelectedColor] = useState('#00D9FF');
  const [sensitivity, setSensitivity] = useState(1.0);
  const [smoothing, setSmoothing] = useState(0.7);
  const [amplitude, setAmplitude] = useState(0);
  const animationFrameRef = useRef<number | undefined>(undefined);

  useEffect(() => {
    requestMicrophonePermission();
    return () => {
      if (isRecording) {
        stopRecording();
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const requestMicrophonePermission = async () => {
    try {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
          {
            title: 'Microphone Permission',
            message:
              'This app needs access to your microphone to visualize audio',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );
        return granted === PermissionsAndroid.RESULTS.GRANTED;
      } else {
        const granted = await AudioVisualizerModule.requestPermission();
        return granted;
      }
    } catch (err) {
      console.warn(err);
      return false;
    }
  };

  const startRecording = async () => {
    try {
      const hasPermission = await requestMicrophonePermission();
      if (!hasPermission) {
        Alert.alert('Permission Denied', 'Microphone permission is required');
        return;
      }

      AudioVisualizerModule.startRecording();
      setIsRecording(true);

      // Start animation loop to update amplitude
      const updateAmplitude = () => {
        try {
          const amp = AudioVisualizerModule.getAmplitude();
          setAmplitude(amp);
        } catch (error) {
          console.error('Error getting amplitude:', error);
        }
        animationFrameRef.current = requestAnimationFrame(updateAmplitude);
      };
      updateAmplitude();
    } catch (error) {
      console.error('Error starting recording:', error);
      Alert.alert('Error', 'Failed to start recording');
    }
  };

  const stopRecording = () => {
    try {
      AudioVisualizerModule.stopRecording();
      setIsRecording(false);
      setAmplitude(0);

      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    } catch (error) {
      console.error('Error stopping recording:', error);
    }
  };

  const toggleRecording = () => {
    if (isRecording) {
      stopRecording();
    } else {
      startRecording();
    }
  };

  return (
    <View
      style={[
        styles.container,
        { paddingTop: insets.top, paddingBottom: insets.bottom },
      ]}
    >
      <StatusBar barStyle="light-content" backgroundColor="#0A0E27" />

      <View style={styles.header}>
        <Text style={styles.title}>🔊 Audio Visualizer</Text>
        <Text style={styles.subtitle}>Native Real-Time Visualization</Text>
      </View>

      {/* Visualizer View */}
      <View style={styles.visualizerContainer}>
        <AudioVisualizerView
          type={selectedStyle}
          color={selectedColor}
          sensitivity={sensitivity}
          smoothing={smoothing}
          style={styles.visualizer}
        />

        {!isRecording && (
          <View style={styles.overlay}>
            <Text style={styles.overlayText}>Tap Start to begin</Text>
          </View>
        )}
      </View>

      {/* Amplitude Indicator */}
      <View style={styles.amplitudeContainer}>
        <Text style={styles.amplitudeLabel}>Amplitude</Text>
        <View style={styles.amplitudeBar}>
          <View
            style={[
              styles.amplitudeFill,
              {
                width: `${amplitude * 100}%`,
                backgroundColor: selectedColor,
              },
            ]}
          />
        </View>
        <Text style={styles.amplitudeValue}>
          {(amplitude * 100).toFixed(1)}%
        </Text>
      </View>

      {/* Controls */}
      <ScrollView style={styles.controls} showsVerticalScrollIndicator={false}>
        {/* Record Button */}
        <TouchableOpacity
          style={[
            styles.recordButton,
            isRecording && styles.recordButtonActive,
            { borderColor: selectedColor },
          ]}
          onPress={toggleRecording}
        >
          <View
            style={[
              styles.recordButtonInner,
              isRecording && styles.recordButtonInnerActive,
              isRecording && { backgroundColor: selectedColor },
            ]}
          />
          <Text style={styles.recordButtonText}>
            {isRecording ? 'Stop Recording' : 'Start Recording'}
          </Text>
        </TouchableOpacity>

        {/* Visualization Style */}
        <Text style={styles.sectionTitle}>Visualization Style</Text>
        <View style={styles.styleGrid}>
          {VISUALIZATION_STYLES.map((style) => (
            <TouchableOpacity
              key={style.name}
              style={[
                styles.styleButton,
                selectedStyle === style.name && styles.styleButtonActive,
                selectedStyle === style.name && { borderColor: selectedColor },
              ]}
              onPress={() => setSelectedStyle(style.name)}
            >
              <Text
                style={[
                  styles.styleButtonText,
                  selectedStyle === style.name && styles.styleButtonTextActive,
                  selectedStyle === style.name && { color: selectedColor },
                ]}
              >
                {style.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* Color Picker */}
        <Text style={styles.sectionTitle}>Color</Text>
        <View style={styles.colorGrid}>
          {COLORS.map((color) => (
            <TouchableOpacity
              key={color.value}
              style={[
                styles.colorButton,
                { backgroundColor: color.value },
                selectedColor === color.value && styles.colorButtonActive,
              ]}
              onPress={() => setSelectedColor(color.value)}
            >
              {selectedColor === color.value && (
                <Text style={styles.colorCheckmark}>✓</Text>
              )}
            </TouchableOpacity>
          ))}
        </View>

        {/* Sensitivity */}
        <Text style={styles.sectionTitle}>
          Sensitivity: {sensitivity.toFixed(1)}x
        </Text>
        <View style={styles.sliderButtons}>
          <TouchableOpacity
            style={styles.sliderButton}
            onPress={() => setSensitivity(Math.max(0.1, sensitivity - 0.1))}
          >
            <Text style={styles.sliderButtonText}>-</Text>
          </TouchableOpacity>
          <View style={styles.sliderBar}>
            <View
              style={[
                styles.sliderFill,
                {
                  width: `${(sensitivity / 2) * 100}%`,
                  backgroundColor: selectedColor,
                },
              ]}
            />
          </View>
          <TouchableOpacity
            style={styles.sliderButton}
            onPress={() => setSensitivity(Math.min(2.0, sensitivity + 0.1))}
          >
            <Text style={styles.sliderButtonText}>+</Text>
          </TouchableOpacity>
        </View>

        {/* Smoothing */}
        <Text style={styles.sectionTitle}>
          Smoothing: {smoothing.toFixed(1)}
        </Text>
        <View style={styles.sliderButtons}>
          <TouchableOpacity
            style={styles.sliderButton}
            onPress={() => setSmoothing(Math.max(0, smoothing - 0.1))}
          >
            <Text style={styles.sliderButtonText}>-</Text>
          </TouchableOpacity>
          <View style={styles.sliderBar}>
            <View
              style={[
                styles.sliderFill,
                {
                  width: `${smoothing * 100}%`,
                  backgroundColor: selectedColor,
                },
              ]}
            />
          </View>
          <TouchableOpacity
            style={styles.sliderButton}
            onPress={() => setSmoothing(Math.min(1.0, smoothing + 0.1))}
          >
            <Text style={styles.sliderButtonText}>+</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.footerSpacing} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0A0E27',
  },
  footerSpacing: {
    height: 40,
  },
  header: {
    padding: 20,
    alignItems: 'center',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#FFFFFF',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 14,
    color: '#8B92B0',
  },
  visualizerContainer: {
    height: 300,
    marginHorizontal: 20,
    marginBottom: 20,
    borderRadius: 20,
    overflow: 'hidden',
    backgroundColor: '#151B3D',
    position: 'relative',
  },
  visualizer: {
    flex: 1,
  },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.3)',
  },
  overlayText: {
    fontSize: 18,
    color: '#FFFFFF',
    fontWeight: '600',
  },
  amplitudeContainer: {
    marginHorizontal: 20,
    marginBottom: 20,
  },
  amplitudeLabel: {
    fontSize: 12,
    color: '#8B92B0',
    marginBottom: 8,
    fontWeight: '600',
  },
  amplitudeBar: {
    height: 8,
    backgroundColor: '#151B3D',
    borderRadius: 4,
    overflow: 'hidden',
    marginBottom: 4,
  },
  amplitudeFill: {
    height: '100%',
    borderRadius: 4,
  },
  amplitudeValue: {
    fontSize: 12,
    color: '#8B92B0',
    textAlign: 'right',
  },
  controls: {
    flex: 1,
    paddingHorizontal: 20,
  },
  recordButton: {
    height: 70,
    borderRadius: 35,
    borderWidth: 3,
    borderColor: '#2A3154',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 24,
    backgroundColor: '#151B3D',
  },
  recordButtonActive: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
  },
  recordButtonInner: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: '#FF4757',
    marginRight: 12,
  },
  recordButtonInnerActive: {
    borderRadius: 4,
  },
  recordButtonText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  sectionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#FFFFFF',
    marginBottom: 12,
    marginTop: 8,
  },
  styleGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
    gap: 8,
  },
  styleButton: {
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: '#151B3D',
    borderWidth: 2,
    borderColor: 'transparent',
  },
  styleButtonActive: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
  },
  styleButtonText: {
    fontSize: 13,
    color: '#8B92B0',
    fontWeight: '600',
  },
  styleButtonTextActive: {
    fontWeight: 'bold',
  },
  colorGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 16,
    gap: 12,
  },
  colorButton: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
  },
  colorButtonActive: {
    borderWidth: 3,
    borderColor: '#FFFFFF',
  },
  colorCheckmark: {
    fontSize: 20,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  sliderButtons: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 16,
    gap: 12,
  },
  sliderButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: '#151B3D',
    justifyContent: 'center',
    alignItems: 'center',
  },
  sliderButtonText: {
    fontSize: 24,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  sliderBar: {
    flex: 1,
    height: 8,
    backgroundColor: '#151B3D',
    borderRadius: 4,
    overflow: 'hidden',
  },
  sliderFill: {
    height: '100%',
    borderRadius: 4,
  },
});

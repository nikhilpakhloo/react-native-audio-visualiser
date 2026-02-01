# @nikhilpakhloo/react-native-audio-visualizer 🎙️✨

A high-performance, real-time audio visualization library for React Native, powered by [Nitro Modules](https://nitro.margelo.com/). Capture live microphone input and render beautiful, fluid visualizations with native performance.

## 🚀 Features

- **Blazing Fast**: Uses Native Nitro Modules for low-latency audio processing and optimized native rendering.
- **7 Premium Visualization Styles**:
  - `strings`: Elegant Siri-inspired wave lines with Gaussian tapering.
  - `bars`: Modern solid DJ-style LED pillars with high-frequency reactivity.
  - `waves`: Fluid, multi-layered colorful waves.
  - `thin-bars`: Minimalist professional recorder-style lines.
  - `particles`: Cinematic floating particles that react to audio energy.
  - `orbit`: Pulsating circular arcs for circular/centered UIs.
  - `waveform`: iOS Voice Memos style scrolling history waveform.
- **Advanced Analysis**: High-resolution 7-band frequency analysis and non-linear amplitude scaling (`sqrt`) for organic movement.
- **Customizable**: Real-time control over color, sensitivity, smoothing, and style.
- **Safe & Clipless**: Built-in vertical padding to ensure visuals never clip on notched devices.

## 📦 Installation

```sh
npm install @nikhilpakhloo/react-native-audio-visualizer react-native-nitro-modules
```

> **Note**: `react-native-nitro-modules` is a required peer dependency.

### Permissions

#### Android
Add this to your `AndroidManifest.xml`:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```

#### iOS
Add this to your `Info.plist`:
```xml
<key>NSMicrophoneUsageDescription</key>
<string>This app needs access to your microphone to visualize audio.</string>
```

## 🛠️ Usage

### 1. The Visualization Component

```tsx
import { AudioVisualizerView } from 'react-native-audio-visualizer';

// ...

<AudioVisualizerView
  type="strings"        // strings, bars, waves, thin-bars, particles, orbit, waveform
  color="#00D9FF"       // Hex color string
  sensitivity={1.0}     // 0.0 - 2.0
  smoothing={0.7}       // 0.0 - 1.0
  style={{ width: '100%', height: 200 }}
/>
```

### 2. Audio Control Module

```tsx
import { AudioVisualizerModule } from 'react-native-audio-visualizer';

// Start capturing audio
const start = async () => {
  const granted = await AudioVisualizerModule.requestPermission();
  if (granted) {
    await AudioVisualizerModule.startRecording();
  }
};

// Stop capturing
const stop = async () => {
  await AudioVisualizerModule.stopRecording();
};

// Check state
const recording = AudioVisualizerModule.isRecording();

// Get real-time data in JS (if needed)
const amp = AudioVisualizerModule.getAmplitude();
const bands = AudioVisualizerModule.getFrequencyBands();
```

## 📖 API Reference

### `AudioVisualizerView` Props

| Prop | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| **`type`** | `VisualizationStyle` | `'strings'` | The visual style to render. |
| **`color`** | `string` | `'#00D9FF'` | Hex color of the visualization. |
| **`sensitivity`** | `number` | `1.0` | Multiplier for the audio amplitude (0.0 to 2.0). |
| **`smoothing`** | `number` | `0.7` | How smoothly the visualization changes (0.0 to 1.0). |

### `AudioVisualizerModule` Methods

- `startRecording(): Promise<void>`
- `stopRecording(): Promise<void>`
- `isRecording(): boolean`
- `getAmplitude(): number` (returns 0.0 - 1.0)
- `getFrequencyBands(): number[]` (returns 7-element array)
- `requestPermission(): Promise<boolean>`

## 📜 License

MIT

---

Made with ❤️ by [nikhilpakhloo](https://github.com/nikhilpakhloo)

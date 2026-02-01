import { NitroModules } from 'react-native-nitro-modules';
import { requireNativeComponent } from 'react-native';
import type { AudioVisualizer } from './AudioVisualizer.nitro';
import type {
  AudioVisualizerViewProps,
  VisualizationStyle,
} from './AudioVisualizerView.nitro';

// Export the HybridObject for audio capture and analysis
export const AudioVisualizerModule =
  NitroModules.createHybridObject<AudioVisualizer>('AudioVisualizer');

// Export the native view component
export const AudioVisualizerView = requireNativeComponent<
  AudioVisualizerViewProps & {
    style?: any;
  }
>('AudioVisualizerView');

// Export types
export type { VisualizationStyle, AudioVisualizerViewProps };

// Convenience functions
export const startRecording = () => AudioVisualizerModule.startRecording();
export const stopRecording = () => AudioVisualizerModule.stopRecording();
export const isRecording = () => AudioVisualizerModule.isRecording();
export const getAmplitude = () => AudioVisualizerModule.getAmplitude();
export const getFrequencyBands = () =>
  AudioVisualizerModule.getFrequencyBands();
export const requestPermission = () =>
  AudioVisualizerModule.requestPermission();

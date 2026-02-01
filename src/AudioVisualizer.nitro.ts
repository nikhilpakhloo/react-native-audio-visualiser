import type { HybridObject } from 'react-native-nitro-modules';

export interface AudioVisualizer
  extends HybridObject<{ ios: 'swift'; android: 'kotlin' }> {
  startRecording(): void;
  stopRecording(): void;
  isRecording(): boolean;
  getAmplitude(): number;
  getFrequencyBands(): number[];
  requestPermission(): Promise<boolean>;
}

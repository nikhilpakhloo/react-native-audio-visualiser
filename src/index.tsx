import { NitroModules } from 'react-native-nitro-modules';
import type { AudioVisualizer } from './AudioVisualizer.nitro';

const AudioVisualizerHybridObject =
  NitroModules.createHybridObject<AudioVisualizer>('AudioVisualizer');

export function multiply(a: number, b: number): number {
  return AudioVisualizerHybridObject.multiply(a, b);
}

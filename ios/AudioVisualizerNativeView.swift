import UIKit

class AudioVisualizerNativeView: UIView {
    
    private var visualizationType = "strings"
    private var visualizationColor = UIColor(hex: "#00D9FF")
    private var sensitivity: CGFloat = 1.0
    private var smoothing: CGFloat = 0.8
    
    private var amplitude: CGFloat = 0
    private var targetAmplitude: CGFloat = 0
    private var frequencyBands: [CGFloat] = Array(repeating: 0, count: 7)
    
    // Waveform history
    private var amplitudeHistory: [CGFloat] = Array(repeating: 0, count: 100)
    private var lastHistoryUpdate: TimeInterval = 0
    
    // Particles management
    private class Particle {
        var x: CGFloat = 0
        var y: CGFloat = 0
        var vx: CGFloat = 0
        var vy: CGFloat = 0
        var size: CGFloat = 0
        var alpha: CGFloat = 0
        
        init(width: CGFloat, height: CGFloat) {
            reset(width: width, height: height)
        }
        
        func reset(width: CGFloat, height: CGFloat) {
            x = CGFloat.random(in: 0...width)
            y = CGFloat.random(in: 0...height)
            vx = CGFloat.random(in: -1...1)
            vy = CGFloat.random(in: -1...1)
            size = CGFloat.random(in: 4...10)
            alpha = CGFloat.random(in: 0.2...0.8)
        }
    }
    private var particles: [Particle] = []
    
    // Animation state
    private var animationPhase: CGFloat = 0
    private var displayLink: CADisplayLink?
    
    // Padding
    private let paddingPercent: CGFloat = 0.15
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setup()
    }
    
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setup()
    }
    
    private func setup() {
        backgroundColor = .clear
        layer.drawsAsynchronously = true
        
        displayLink = CADisplayLink(target: self, selector: #selector(updateAnimation))
        displayLink?.add(to: .main, forMode: .common)
    }
    
    deinit {
        displayLink?.invalidate()
    }
    
    @objc private func updateAnimation() {
        // Smooth amplitude transition
        amplitude += (targetAmplitude - amplitude) * (1 - smoothing)
        
        // Add breathing noise
        let breathing = max(0, sin(animationPhase * 0.5) * 0.02)
        animationPhase += 0.08
        
        // Update history for waveform
        let now = CACurrentMediaTime()
        if now - lastHistoryUpdate > 0.05 {
            amplitudeHistory.removeFirst()
            amplitudeHistory.append(amplitude + breathing)
            lastHistoryUpdate = now
        }
        
        setNeedsDisplay()
    }
    
    func setType(_ type: String) {
        visualizationType = type
    }
    
    func setColor(_ color: String) {
        visualizationColor = UIColor(hex: color)
    }
    
    func setSensitivity(_ value: CGFloat) {
        sensitivity = max(0, min(2, value))
    }
    
    func setSmoothing(_ value: CGFloat) {
        smoothing = max(0, min(1, value))
    }
    
    func updateAmplitude(_ amp: CGFloat) {
        targetAmplitude = max(0, min(1, amp * sensitivity))
    }
    
    func updateFrequencyBands(_ bands: [CGFloat]) {
        if bands.count >= frequencyBands.count {
            for i in 0..<frequencyBands.count {
                frequencyBands[i] = bands[i]
            }
        }
    }
    
    override func draw(_ rect: CGRect) {
        guard let context = UIGraphicsGetCurrentContext() else { return }
        let breathing = max(0, sin(animationPhase * 0.5) * 0.02)
        
        switch visualizationType {
        case "strings":
            drawStrings(context: context, rect: rect, breathing: breathing)
        case "bars":
            drawBars(context: context, rect: rect, breathing: breathing)
        case "waves":
            drawWaves(context: context, rect: rect, breathing: breathing)
        case "thin-bars":
            drawThinBars(context: context, rect: rect, breathing: breathing)
        case "particles":
            drawParticles(context: context, rect: rect, breathing: breathing)
        case "orbit":
            drawOrbit(context: context, rect: rect, breathing: breathing)
        case "waveform":
            drawWaveform(context: context, rect: rect)
        default:
            drawStrings(context: context, rect: rect, breathing: breathing)
        }
    }
    
    private func drawStrings(context: CGContext, rect: CGRect, breathing: CGFloat) {
        let centerY = rect.height / 2
        let numWaves = 5
        let usableHeight = rect.height * (1 - paddingPercent * 2)
        
        context.setLineCap(.round)
        context.setLineJoin(.round)
        
        for i in 0..<numWaves {
            let alpha = (0.6 * (1 - CGFloat(i) / CGFloat(numWaves)) + 0.15 * sin(animationPhase + CGFloat(i)))
            let color = visualizationColor.withAlphaComponent(max(0.1, alpha))
            
            let speed = animationPhase * (0.6 + CGFloat(i) * 0.2)
            let freq = 0.012 + CGFloat(i) * 0.004
            
            let path = UIBezierPath()
            path.move(to: CGPoint(x: 0, y: centerY))
            
            let steps = 60
            let stepWidth = rect.width / CGFloat(steps)
            
            for step in 0...steps {
                let x = CGFloat(step) * stepWidth
                let distFromCenter = abs(CGFloat(step) - CGFloat(steps) / 2) / (CGFloat(steps) / 2)
                let envelope = exp(-distFromCenter * distFromCenter * 5)
                
                let angle = x * freq + speed
                let waveAmp = (amplitude + breathing) * usableHeight * 0.5 * (1 - CGFloat(i) * 0.15)
                let y = centerY + sin(angle) * waveAmp * envelope
                path.addLine(to: CGPoint(x: x, y: y))
            }
            
            context.setStrokeColor(color.cgColor)
            context.setLineWidth(max(1.5, 4 - CGFloat(i) * 0.5))
            
            // Glow effect
            context.setShadow(offset: .zero, blur: 15, color: visualizationColor.cgColor)
            
            context.addPath(path.cgPath)
            context.strokePath()
        }
    }
    
    private func drawBars(context: CGContext, rect: CGRect, breathing: CGFloat) {
        let numBars = 70
        let barWidth = rect.width / CGFloat(numBars)
        let gap = barWidth * 0.4
        let actualBarWidth = max(3, barWidth - gap)
        let centerY = rect.height / 2
        let usableHeight = rect.height * (1 - paddingPercent * 2)
        
        context.setShadow(offset: .zero, blur: 10, color: visualizationColor.cgColor)
        context.setFillColor(visualizationColor.cgColor)
        
        for i in 0..<numBars {
            let relativeIdx = i < numBars / 2 ? i : (numBars - 1 - i)
            let progress = CGFloat(relativeIdx) / (CGFloat(numBars) / 2)
            
            let bandIdx = Int(progress * CGFloat(frequencyBands.count - 1))
            let bandLow = frequencyBands[bandIdx]
            let bandHigh = frequencyBands[min(bandIdx + 1, frequencyBands.count - 1)]
            let interpol = (progress * CGFloat(frequencyBands.count - 1)) - CGFloat(bandIdx)
            
            let value = lerp(start: bandLow, stop: bandHigh, amount: interpol)
            
            // Variation to prevent flat top
            let variation = 0.85 + 0.15 * sin(CGFloat(i) * 0.4 + animationPhase)
            let h = (value * usableHeight * variantScaling(i) * variation + 12 * (amplitude + breathing)).clamped(8, usableHeight)
            
            let x = CGFloat(i) * barWidth + gap / 2
            let barRect = CGRect(x: x, y: centerY - h / 2, width: actualBarWidth, height: h)
            let path = UIBezierPath(roundedRect: barRect, cornerRadius: actualBarWidth / 2)
            
            context.addPath(path.cgPath)
            context.fillPath()
        }
    }
    
    private func drawWaves(context: CGContext, rect: CGRect, breathing: CGFloat) {
        let baseLine = rect.height * 0.7
        let usableHeight = rect.height * (1 - paddingPercent)
        
        for i in 0..<3 {
            let alpha = 0.35 + CGFloat(i) * 0.25
            let color = visualizationColor.withAlphaComponent(min(1.0, alpha))
            
            let speed = animationPhase * (0.5 + CGFloat(i) * 0.25)
            let freq = 0.007 + CGFloat(i) * 0.003
            let layerOffset = CGFloat(i) * (rect.height * 0.05)
            
            let path = UIBezierPath()
            path.move(to: CGPoint(x: 0, y: rect.height))
            path.addLine(to: CGPoint(x: 0, y: baseLine + layerOffset))
            
            let steps = 40
            let stepWidth = rect.width / CGFloat(steps)
            
            for step in 0...steps {
                let x = CGFloat(step) * stepWidth
                let angle = x * freq + speed
                let y = (baseLine + layerOffset) + sin(angle) * (amplitude + breathing) * (rect.height * 0.2)
                path.addLine(to: CGPoint(x: x, y: min(y, rect.height - 5)))
            }
            
            path.addLine(to: CGPoint(x: rect.width, y: rect.height))
            path.close()
            
            context.setFillColor(color.cgColor)
            context.addPath(path.cgPath)
            context.fillPath()
        }
    }
    
    private func drawThinBars(context: CGContext, rect: CGRect, breathing: CGFloat) {
        let numBars = 100
        let barWidth = rect.width / CGFloat(numBars)
        let actualBarWidth: CGFloat = 2
        let gap = barWidth - actualBarWidth
        let centerY = rect.height / 2
        let usableHeight = rect.height * (1 - paddingPercent * 2)
        
        context.setFillColor(visualizationColor.cgColor)
        context.setShadow(offset: .zero, blur: 5, color: visualizationColor.cgColor)
        
        for i in 0..<numBars {
            let progress = CGFloat(i) / CGFloat(numBars)
            let bandIdx = Int(progress * CGFloat(frequencyBands.count - 1))
            let bandLow = frequencyBands[bandIdx]
            let bandHigh = frequencyBands[min(bandIdx + 1, frequencyBands.count - 1)]
            let interpol = (progress * CGFloat(frequencyBands.count - 1)) - CGFloat(bandIdx)
            let value = lerp(start: bandLow, stop: bandHigh, amount: interpol)
            
            let variation = 0.8 + 0.2 * sin(CGFloat(i) * 0.8 + animationPhase)
            let h = (value * usableHeight * variation + 6 * (amplitude + breathing)).clamped(4, usableHeight)
            let x = CGFloat(i) * barWidth + gap / 2
            
            context.fill(CGRect(x: x, y: centerY - h / 2, width: actualBarWidth, height: h))
        }
    }
    
    private func drawParticles(context: CGContext, rect: CGRect, breathing: CGFloat) {
        if particles.isEmpty {
            for _ in 0..<40 { particles.append(Particle(width: rect.width, height: rect.height)) }
        }
        
        context.setShadow(offset: .zero, blur: 12, color: visualizationColor.cgColor)
        
        for p in particles {
            p.x += p.vx * (1 + amplitude * 5)
            p.y += p.vy * (1 + amplitude * 5)
            
            if p.x < 0 { p.x = rect.width }
            if p.x > rect.width { p.x = 0 }
            if p.y < 0 { p.y = rect.height }
            if p.y > rect.height { p.y = 0 }
            
            let size = p.size * (1 + amplitude * 1.5)
            context.setFillColor(visualizationColor.withAlphaComponent(p.alpha).cgColor)
            context.fillEllipse(in: CGRect(x: p.x - size/2, y: p.y - size/2, width: size, height: size))
        }
    }
    
    private func drawOrbit(context: CGContext, rect: CGRect, breathing: CGFloat) {
        let center = CGPoint(x: rect.width / 2, y: rect.height / 2)
        let usableRadius = min(rect.width, rect.height) * 0.4
        
        context.setShadow(offset: .zero, blur: 15, color: visualizationColor.cgColor)
        
        for i in 0..<4 {
            let radius = (usableRadius * 0.4) + (CGFloat(i) * 15) + (amplitude * usableRadius * 0.5)
            let alpha = 1.0 - CGFloat(i) * 0.2
            
            let path = UIBezierPath(arcCenter: center,
                                    radius: radius,
                                    startAngle: (animationPhase * 3.0 + CGFloat(i) * .pi / 2),
                                    endAngle: (animationPhase * 3.0 + CGFloat(i) * .pi / 2) + .pi / 3 + amplitude * .pi,
                                    clockwise: true)
            
            context.setStrokeColor(visualizationColor.withAlphaComponent(max(0.1, alpha)).cgColor)
            context.setLineWidth(max(1, 5 - CGFloat(i)))
            context.addPath(path.cgPath)
            context.strokePath()
            
            let path2 = UIBezierPath(arcCenter: center,
                                     radius: radius,
                                     startAngle: (animationPhase * 3.0 + CGFloat(i) * .pi / 2) + .pi,
                                     endAngle: (animationPhase * 3.0 + CGFloat(i) * .pi / 2) + .pi + .pi / 3 + amplitude * .pi,
                                     clockwise: true)
            context.addPath(path2.cgPath)
            context.strokePath()
        }
    }
    
    private func drawWaveform(context: CGContext, rect: CGRect) {
        let barWidth = rect.width / CGFloat(amplitudeHistory.count)
        let gap = barWidth * 0.3
        let actualBarWidth = max(2, barWidth - gap)
        let centerY = rect.height / 2
        let usableHeight = rect.height * (1 - paddingPercent * 2)
        
        context.setFillColor(visualizationColor.cgColor)
        context.setShadow(offset: .zero, blur: 8, color: visualizationColor.cgColor)
        
        for i in 0..<amplitudeHistory.count {
            let amp = amplitudeHistory[i]
            let h = (amp * usableHeight + 4).clamped(4, usableHeight)
            let x = CGFloat(i) * barWidth + gap / 2
            
            let barRect = CGRect(x: x, y: centerY - h / 2, width: actualBarWidth, height: h)
            let path = UIBezierPath(roundedRect: barRect, cornerRadius: actualBarWidth / 2)
            context.addPath(path.cgPath)
            context.fillPath()
        }
    }
    
    private func lerp(start: CGFloat, stop: CGFloat, amount: CGFloat) -> CGFloat {
        return start + (stop - start) * amount
    }
    
    private func variantScaling(_ index: Int) -> CGFloat {
        return 0.9 + 0.1 * sin(CGFloat(index) * 0.1)
    }
}

extension UIColor {
    convenience init(hex: String) {
        var hexSanitized = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        hexSanitized = hexSanitized.replacingOccurrences(of: "#", with: "")
        var rgb: UInt64 = 0
        Scanner(string: hexSanitized).scanHexInt64(&rgb)
        let r = CGFloat((rgb & 0xFF0000) >> 16) / 255.0
        let g = CGFloat((rgb & 0x00FF00) >> 8) / 255.0
        let b = CGFloat(rgb & 0x0000FF) / 255.0
        self.init(red: r, green: g, blue: b, alpha: 1.0)
    }
}

extension CGFloat {
    func clamped(_ minV: CGFloat, _ maxV: CGFloat) -> CGFloat {
        return max(minV, min(maxV, self))
    }
}

[![](https://jitpack.io/v/magneticflux-/Marionette.svg)](https://jitpack.io/#magneticflux-/Marionette)

# Marionette

Marionette is designed to train neural networks to play NES game, in a fashion akin to a marionette. The name is also a play on the words "Mario" and "Net", referencing the first supported NES game. Eventually, I hope to train neural networks on the raw visual data, instead of the contents of the NES's RAM.

Marionette uses the NEAT algorithm detailed in Kenneth Stanley's paper, "[Evolving Neural Networks through
Augmenting Topologies](http://nn.cs.utexas.edu/downloads/papers/stanley.ec02.pdf)".

Supported Games:
- Super Mario Bros. (1987)

## Important!

This project must be built with [Java-NEAT](https://github.com/magneticflux-/Java-NEAT), [JNSGA-II](https://github.com/magneticflux-/JNSGA-II), and [halfnes-headless](https://github.com/magneticflux-/halfnes-headless). I organize it by making a Gradle super-project to build all modules together.

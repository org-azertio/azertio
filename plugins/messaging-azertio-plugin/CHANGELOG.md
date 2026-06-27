# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-06-27

### Added
- JMS messaging support: publish and subscribe on queues and topics
- JSON field extraction from received messages into variables
- Variable interpolation in published message bodies
- Plain-text and JSON message assertions (contains / exact match)
- Publish with routing key
- Configurable per-system connection factory and broker URL
- Configurable receive timeout
- Steps available in English, Spanish and DSL
- ActiveMQ Classic example project (`examples/messaging-example`)
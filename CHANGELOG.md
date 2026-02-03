## Changelog

### Version 1.2.0
- **New Feature**: Added `AuditDiffCalculator` utility for deep recursive diffing of objects.
- **New Feature**: Added support for Asynchronous and Virtual Thread logging via manual `StackTraceElement` injection.
- **Enhancement**: Implemented reflection-based Logback integration to preserve correct Class, Method, and Line numbers in async contexts.
- **Enhancement**: Added overloaded `log` method to accept caller stack frame data.
- **Performance**: Optimized logging overhead using cached reflection handles.
- **Improvement**: Enhanced JSON diff output by automatically converting Jackson nodes to Java primitives.

### Version 1.1
- Add tenantId to audit logs payload
- Improve data sanitation in type and level of logs for consistency

### Version 1.0
- Initial release of CX Audit Logger
- Core audit logging functionality
- JSON serialization support
- Error handling implementation
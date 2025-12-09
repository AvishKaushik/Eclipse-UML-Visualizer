# Eclipse UML Visualizer

A powerful Eclipse IDE plugin that automatically generates interactive UML class diagrams and sequence diagrams from Java source code. Built with a clean 7-layer architecture, it provides comprehensive visualization of your Java projects with support for relationships, external dependencies, and collaborative note-taking.

## Overview

The Eclipse UML Visualizer analyzes Java projects using Eclipse JDT (Java Development Tools) and transforms the code structure into interactive visual diagrams. It supports both class diagrams for understanding system architecture and sequence diagrams for visualizing method call flows.

## Features

### UML Class Diagram Visualization
- **Automatic Class Detection**: Analyzes your entire Java project and visualizes all classes, interfaces, and enums with complete metadata (fields, methods, visibility modifiers)
- **Comprehensive Relationship Detection**:
  - **Inheritance**: Detects `extends` relationships between classes
  - **Interface Implementation**: Shows `implements` relationships
  - **Association**: Field-based relationships between classes
  - **Aggregation**: Non-final field references indicating weak ownership
  - **Composition**: Final field references indicating strong ownership
  - **Bidirectional Relationships**: Automatically curves connections when relationships exist in both directions
- **Interactive Diagrams**:
  - Multiple layout algorithms (Spring, Tree, Horizontal Tree, Grid)
  - Zoom in/out controls with dynamic canvas sizing
  - Orthogonal line routing for clean, professional-looking connections
  - Selection-based actions and interactive node manipulation
  - Dark mode compatible with white background override
- **External Dependency Visualization**: Groups external library imports by package namespace
- **Export Capabilities**: Export diagrams to PNG format with or without notes

### Sequence Diagram Generation
- **Method Call Visualization**: Automatically traces method invocations and participant interactions within your code
- **Project-Wide Analysis**: Generate sequence diagrams for entire projects
- **Smart Filtering**: Automatically excludes library classes and only shows user-created classes from your src folder
- **PlantUML Integration**: Leverages industry-standard PlantUML for generating sequence diagram images
- **Comprehensive Call Types**:
  - Synchronous method calls
  - Object creation/destruction
  - Method chaining and self-calls
- **Smart Participant Ordering**: Intelligent ordering of participants for better readability
- **Zoom Controls**: Zoom in/out to view diagram details
- **PNG Export**: Export sequence diagrams as PNG images

### Collaborative Notes System
- **Sticky Notes**: Add visual sticky notes to classes and relationships for documentation
- **Note Replies**: Support for threaded discussions on notes
- **Persistent Storage**: Notes saved as Git-shareable JSON in `.uml-notes/notes.json`
- **Flexible Management**:
  - Mark notes as active/inactive (grayed out when inactive)
  - Delete obsolete notes
  - Orphaned note handling for deleted elements
  - Visual connection from notes to diagram elements with dashed lines
- **Export Options**: Choose to export diagrams with or without notes

### Real-Time Updates
- Refresh diagrams as your code changes
- Incremental note updates without full regeneration
- Lazy loading for performance optimization

## Installation

### Requirements
- **Eclipse IDE**: 2020-03 or later
- **Java**: JavaSE-21 or higher
- **Eclipse Components** (included in most Eclipse distributions):
  - Eclipse Zest (graph visualization)
  - Eclipse GEF (graph editing framework)
  - Eclipse JDT (Java Development Tools)

### From Eclipse Marketplace
1. Open Eclipse IDE
2. Navigate to `Help` → `Eclipse Marketplace`
3. Search for "Eclipse UML Visualizer"
4. Click `Install` and follow the installation wizard
5. Restart Eclipse when prompted

### From Update Site
1. Open Eclipse IDE
2. Navigate to `Help` → `Install New Software`
3. Click `Add` to add a new repository
4. Enter the update site URL (provided by your administrator or from the project website)
5. Select "Eclipse UML Visualizer" from the available software list
6. Click `Next` and complete the installation wizard
7. Restart Eclipse when prompted

### Manual Installation (Development)
1. Clone this repository
2. Import the project into Eclipse as an existing project
3. Ensure you have Plugin Development Environment (PDE) installed
4. Right-click the project → `Run As` → `Eclipse Application` to launch a test instance

## Usage

### Generating UML Class Diagrams
1. **Open the View**: `Window` → `Show View` → `Other` → `UML Visualizer` → `UML Class Diagram`
2. **Select Project**: Open a Java project in your Eclipse workspace
3. **Generate Diagram**: The diagram automatically generates showing:
   - All classes, interfaces, and enums
   - Fields and methods with visibility indicators
   - Inheritance and implementation relationships
   - Association, aggregation, and composition relationships
   - External dependencies grouped by package
4. **Interact with Diagram**:
   - **Change Layout**: Use toolbar buttons to switch between Spring, Tree, Horizontal Tree, and Grid layouts
   - **Zoom**: Use `+` and `-` buttons to zoom in/out
   - **Pan**: Click and drag to pan across the diagram
   - **Select Elements**: Click on classes or relationships to select them
   - **Add Notes**: Click "Add Note", then click on a class or relationship to attach
   - **Export**: Click "Export PNG" to save the diagram as an image

### Generating Sequence Diagrams
1. **Open the View**: `Window` → `Show View` → `Other` → `UML Visualizer` → `Sequence Diagram`
2. **Generate Diagram**: Click "Generate Project Diagram" to analyze all user-created classes in the selected project
   - Automatically filters out library classes (java.*, org.eclipse.*, etc.)
   - Only includes classes from your src folder
   - Excludes test classes and utility method calls (getters/setters)
3. **View Diagram**: The sequence diagram displays as an image showing:
   - Participants (your classes/objects only)
   - Method calls with chronological ordering
   - Object creation and destruction
4. **Interact with Diagram**:
   - **Zoom**: Use `+` and `-` buttons to zoom in/out
   - **Refresh**: Update the diagram after code changes
   - **Export PNG**: Save the diagram as an image file

### Working with Notes
1. **Add Note**: Click the "Add Note" button in the toolbar
2. **Attach to Element**: Click on a class or relationship in the diagram
3. **Enter Content**: Type your note text in the dialog and save
4. **Manage Notes**:
   - **Reply**: Select a note and click "Reply" to add threaded responses
   - **Mark Inactive**: Gray out notes that are no longer relevant
   - **Delete**: Remove notes permanently
   - **View All**: See all notes and their replies in a list
5. **Export**: Choose whether to include notes when exporting to PNG

## Architecture

### 7-Layer Design
The plugin follows a clean layered architecture:

1. **Presentation Layer** (`views`): Eclipse view integration and user interaction
2. **Visualization Layer** (`core.visualization`): Zest graph rendering and PlantUML generation
3. **Business Logic Layer** (`core.model`): Domain models (ClassNode, Relation, SequenceDiagram, Note)
4. **Analysis & Detection Layer** (`core.parser`): Relationship detection algorithms
5. **Parsing Layer** (`core.parser`): Eclipse JDT AST processing
6. **Persistence Layer** (`core.persistence`): JSON storage for notes and configuration
7. **Data Access Layer**: Eclipse workspace and file system access

### Key Components

#### Data Models (`core.model`)
- **IntermediateRepresentation (IR)**: Central data structure containing all classes and relationships
- **ClassNode**: Represents a Java class with fields, methods, and package information
- **Relation**: Represents relationships (inheritance, implements, association, aggregation, composition)
- **SequenceDiagram**: Method call sequences with participants and messages
- **Note/NoteReply**: Sticky note system with timestamps and author tracking

#### Parsers (`core.parser`)
- **JavaClassParser**: Main AST parser using Eclipse JDT
- **InheritanceDetector**: Analyzes superclasses and interface implementations
- **AssociationDetector**: Detects field-based relationships with composition vs aggregation logic
- **ExternalDependencyDetector**: Groups external imports by package
- **MethodCallParser**: Analyzes method bodies for sequence diagram generation

#### Visualization (`core.visualization`)
- **ClassDiagramGenerator**: Transforms IR into interactive Zest graph with UML-standard visual decorations
- **PlantUMLGenerator**: Converts SequenceDiagram model to PlantUML syntax
- **SequenceDiagramRenderer**: Displays sequence diagrams

#### Persistence (`core.persistence`)
- **NoteStorage**: JSON serialization using Gson, stores notes in `.uml-notes/notes.json`
- **ConfigurationStorage**: Eclipse Preferences API for plugin settings

#### Export (`core.export`)
- **DiagramExporter**: PNG export using SWT graphics context

### Design Patterns
- **MVC Pattern**: Separation of model (IR), view (Zest graph), and controller (actions)
- **Visitor Pattern**: Eclipse AST traversal for parsing
- **Builder Pattern**: IRBuilder for constructing intermediate representation
- **Strategy Pattern**: Multiple layout algorithms
- **Observer Pattern**: Zest selection and interaction events
- **Facade Pattern**: ClassDiagramGenerator simplifies Zest API complexity

## Technology Stack

### Eclipse Platform Dependencies
| Component | Version | Purpose |
|-----------|---------|---------|
| org.eclipse.core.runtime | Latest | Plugin lifecycle and extension registry |
| org.eclipse.ui | Latest | Views, actions, menus, toolbars |
| org.eclipse.swt | Latest | Native widget toolkit |
| org.eclipse.jface | Latest | Dialogs, viewers, data binding |
| org.eclipse.gef | 3.11.0+ | Graph node manipulation |
| org.eclipse.zest.core | 1.5.0+ | Graph visualization framework |
| org.eclipse.zest.layouts | 1.1.0+ | Layout algorithms (Spring, Tree, Grid) |
| org.eclipse.jdt.core | Latest | Java AST parsing and analysis |
| org.eclipse.core.resources | Latest | Workspace and project access |
| org.eclipse.draw2d | Latest | 2D graphics and decorations |

### Third-Party Libraries
- **Gson 2.10.1** (Apache 2.0): JSON serialization for notes
- **PlantUML** (Apache 2.0 compatible): Sequence diagram syntax generation
- **JUnit 4.13.2** (EPL-2.0): Unit testing framework

### Development Tools
- **Java**: JavaSE-21
- **Build System**: Eclipse PDE (Plugin Development Environment)
- **Version Control**: Git

## How It Works

### Class Diagram Generation Pipeline

```
1. User triggers "Generate UML"
   ↓
2. Get selected Java Project from workspace (IJavaProject)
   ↓
3. For each .java file:
   └─→ JavaClassParser creates Eclipse AST
   └─→ Extract ClassInfo (name, fields, methods, package, modifiers)
   ↓
4. IRBuilder converts each ClassInfo to ClassNode
   └─→ Add to IntermediateRepresentation (IR)
   ↓
5. InheritanceDetector analyzes superclasses and interfaces
   └─→ Create Relation objects with type INHERITANCE or IMPLEMENTS
   ↓
6. AssociationDetector analyzes field types
   └─→ Create Relation objects with type ASSOCIATION, AGGREGATION, or COMPOSITION
   └─→ Final fields = COMPOSITION, non-final = AGGREGATION
   ↓
7. ExternalDependencyDetector groups imports
   └─→ Create package nodes for external dependencies
   ↓
8. NoteStorage loads saved notes from .uml-notes/notes.json
   ↓
9. ClassDiagramGenerator creates Zest Graph:
   ├─→ GraphNode for each ClassNode (styled with UML formatting)
   ├─→ GraphConnection for each Relation (with UML-standard arrow decorations)
   ├─→ Note nodes (yellow sticky notes with dashed connections)
   └─→ External package nodes (grouped dependencies)
   ↓
10. Apply selected layout algorithm (Spring/Tree/Grid)
    ↓
11. Display in UMLDiagramView with interactive controls
    └─→ User can zoom, pan, add notes, change layout, export
```

### Sequence Diagram Generation Pipeline

```
1. User clicks "Generate Project Diagram"
   ↓
2. Get all user-created classes from src folder
   ├─→ Filter out library classes (java.*, org.eclipse.*, etc.)
   ├─→ Filter out test classes
   └─→ Only include source folder classes
   ↓
3. Set whitelist of user classes in MethodCallParser
   ↓
4. For each user class:
   └─→ MethodCallParser creates Eclipse AST
   └─→ AST Visitor traverses MethodInvocation nodes
   └─→ Filter out calls to library classes and utility methods
   ↓
5. Build SequenceDiagram model:
   ├─→ Add participants (only user classes)
   └─→ Add messages (method calls, object creation)
   ↓
6. PlantUMLGenerator converts to PlantUML syntax
   └─→ @startuml ... participant declarations ... method calls ... @enduml
   ↓
7. SequenceDiagramRenderer renders PlantUML to image
   ↓
8. Display in SequenceDiagramView with zoom controls
   ↓
9. User can export as PNG image
```

### Note Management Pipeline

```
1. User clicks "Add Note" button
   ↓
2. User selects diagram element (class or relationship)
   ↓
3. NoteDialog opens for text entry
   ↓
4. Note object created with:
   ├─→ Content text
   ├─→ Timestamp
   ├─→ Linked element ID
   └─→ Author (system user)
   ↓
5. NoteStorage.saveNotes() serializes to JSON
   └─→ File: .uml-notes/notes.json (Git-shareable)
   ↓
6. ClassDiagramGenerator.addSingleNote() creates visual note:
   ├─→ Yellow rectangular node
   └─→ Dashed line connecting to element
   ↓
7. Note persists across sessions and can be:
   ├─→ Replied to (threaded discussion)
   ├─→ Marked inactive (grayed out)
   └─→ Deleted (removed from storage)
```

## Supported Diagram Elements

### Class Diagrams
- **Class Types**: Classes, Abstract Classes, Interfaces, Enums
- **Members**: Fields and Methods with full signatures
- **Visibility**: Public (+), Private (-), Protected (#), Package (~)
- **Relationships**:
  - Inheritance (solid line with hollow triangle)
  - Interface Implementation (dashed line with hollow triangle)
  - Association (solid line with arrow)
  - Aggregation (solid line with hollow diamond)
  - Composition (solid line with filled diamond)
- **Additional Elements**:
  - Bidirectional relationship curves
  - External dependency packages
  - Sticky notes with threaded replies

### Sequence Diagrams
- Synchronous method calls (between user classes only)
- Object creation (`new` keyword)
- Method chaining
- Self-calls (recursive methods)
- **Filtered Content**:
  - Library classes are automatically excluded (java.*, javax.*, org.eclipse.*, etc.)
  - Only user-created classes from src folder are shown
  - Utility methods (getters/setters) are filtered out for clarity

## Data Persistence

### Notes Storage
- **Format**: JSON with pretty-printing
- **Location**: `.uml-notes/notes.json` in project root
- **Sharing**: Git-compatible for team collaboration
- **Structure**:
  ```json
  {
    "notes": [
      {
        "id": "uuid",
        "elementId": "className",
        "content": "Note text",
        "author": "username",
        "timestamp": "2024-11-28T10:30:00",
        "active": true,
        "replies": [...]
      }
    ]
  }
  ```

### Configuration
- **Storage**: Eclipse Preferences API
- **Scope**: Workspace-level preferences
- **Settings**: Layout preferences, export options, view configurations

## Performance Optimizations

- **Incremental Updates**: Notes added without full diagram regeneration
- **Lazy Loading**: Classes loaded on-demand
- **Canvas Scaling**: Square-root scaling prevents excessive canvas size for large projects
- **AST Caching**: Parsed ASTs cached during session
- **Orphaned Note Handling**: Efficient detection of notes for deleted elements
- **Smart Filtering**: Whitelist-based filtering in sequence diagrams reduces memory usage and diagram complexity

## License

This project is licensed under the **Eclipse Public License 2.0 (EPL-2.0)**.

### Third-Party Licenses
- **Gson**: Apache License 2.0
- **PlantUML**: Multi-licensed (Apache 2.0 compatible)
- **Eclipse Platform**: Eclipse Public License 2.0
- **JUnit**: Eclipse Public License 2.0

All licenses are compatible for commercial and open-source use.

## Development

### Building from Source
1. Clone the repository: `git clone <repository-url>`
2. Import into Eclipse: `File` → `Import` → `Existing Projects into Workspace`
3. Ensure Plugin Development Environment (PDE) is installed
4. Build: `Project` → `Build Project`
5. Run tests: Right-click test classes → `Run As` → `JUnit Test`
6. Launch: Right-click project → `Run As` → `Eclipse Application`

### Running Tests
```bash
# Unit tests are located in test/ subdirectories within each package:
# - core.model.test.*Test
# - core.parser.test.*Test
```

Run all tests from Eclipse: Right-click `src` folder → `Run As` → `JUnit Test`

### Project Structure
```
Eclipse-UML-Visualizer/
├── src/
│   ├── core/
│   │   ├── model/          # Domain models and IR
│   │   ├── parser/         # AST parsing and detection
│   │   ├── visualization/  # Zest and PlantUML rendering
│   │   ├── persistence/    # JSON storage
│   │   └── export/         # PNG export
│   └── views/              # Eclipse view integration
├── lib/
│   ├── gson-2.10.1.jar
│   └── plantuml.jar
├── META-INF/
│   └── MANIFEST.MF         # OSGi bundle manifest
├── plugin.xml              # Eclipse extension points
├── build.properties        # Build configuration
└── .classpath              # Eclipse classpath
```

## Contributing

Contributions are welcome! Here's how you can help:

1. **Report Bugs**: Open an issue with detailed reproduction steps
2. **Suggest Features**: Describe your use case and proposed solution
3. **Submit Pull Requests**:
   - Fork the repository
   - Create a feature branch
   - Write tests for new functionality
   - Submit PR with clear description
4. **Improve Documentation**: Help make the README and code comments better

### Development Guidelines
- Follow Eclipse coding conventions
- Write JUnit tests for new features
- Update documentation for user-facing changes
- Use meaningful commit messages
- Maintain the 7-layer architecture separation

## Support

- **Issues**: Report bugs and request features on the GitHub repository
- **Documentation**: Refer to this README and inline code documentation
- **Eclipse Help**: Use Eclipse's built-in help system for plugin development

## Version History

### 1.0.0 (Current)
- Initial release
- UML class diagram generation with 5 relationship types
- Sequence diagram generation with PlantUML integration
- Collaborative notes system with JSON persistence
- Multiple layout algorithms (Spring, Tree, Horizontal Tree, Grid)
- PNG export functionality
- External dependency visualization
- Bidirectional relationship handling
- Interactive Zest graph with zoom and pan
- Eclipse 2020-03+ and Java 21 support

## Acknowledgments

- **Eclipse Foundation**: For the robust plugin development platform and Java analysis tools
- **PlantUML**: For sequence diagram syntax generation
- **Gson**: For lightweight JSON serialization
- **Eclipse Zest & GEF**: For powerful graph visualization capabilities
- **Open Source Community**: For continuous inspiration and support

---

**Made with ❤️ for the Eclipse community**

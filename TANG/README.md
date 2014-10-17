Tang is a configuration managment and checking framework that  emphasizes explicit documentation and automatic checkability of configurations and applications instead of ad-hoc, application-specific configuration and bootstrapping logic.  It supports distributed, multi-language applications, but gracefully handles simpler use cases as well.

Tang makes use of dependency injection to automatically instantiate applications.  Dependency injectors can be thought of as "make for objects"--given a request for some type of object, and information that explains how dependencies between objects should be resolved, dependency injectors automatically instantiate the requested object and all of the objects it dependes upon.  Tang makes use of a few simple wire formats to support remote and even cross-language dependency injection.

Outline
-------

   * [Motivation](#motivation)
   * [Design principles](#design-principles)
   * [Tutorial: Getting started](#tutorial-getting-started)
     * [Defining configuration parameters](#configuration-parameters)
     * [Configuration Modules](#configuration-modules)
     * [Raw configuration API](#raw-configuration-api)
   * [Tutorial: Complex application architectures](#tutorial-complex-application-architectures)
     * [Distributed dependency injection](#distributed-dependency-injection)
     * [Dynamically setting parameters and choosing implementations](#bind)
     * [Creating sets of similar injectors](#child-injectors)
   * [Tutorial: Design patterns and best practices](#tutorial-design-patterns-and-best-practices)
     * [Modularity and complex configurations](#modularity-and-complex-configurations)
     * [Cyclic dependencies](#cyclic-dependencies)
   * [Roadmap](#roadmap)
     * [Using the injection plan API to choose between multiple implementations](#injection-plans)
     * [Language interoperability](#language-interoperability)

Motivation
============

Distributed systems suffer from problems that arise due to complex compositions of software modules and configuration errors.  These problems compound over time: best-practice object oriented design dictates that code be factored into independent reusable modules, and today's distributed applications are increasingly expected to run atop multiple runtime environments.  This leads application developers to push complexity into configuration settings, to the point where misconfiguration is now a primary cause of unavailability in fault tolerant systems.

Tang is our attempt to address these problems.  It consists of a dependency injection framework and a set of configuration and debugging tools that automatically and transparently bootstrap applications.  We have focused on providing a narrow set of primitives that support the full range of design patterns that arise in distributed system development, and that encourage application developers to build their systems in a maintainable and debuggable way.

Tang leverages existing language type systems, allowing unmodified IDEs such as Eclipse or NetBeans to surface configuration information in tooltips, provide auto-complete of configuration parameters, and to detect a wide range of configuration problems as you edit your code.  Since such functionality is surfaced in the tools you are already familiar with, there is no need to install (or learn) additional development software to get started with Tang.  Furthermore, we provide a set of sophisticated build time and runtime tools that detect a wide range of common architectural problems and configuration errors.

This documentation consists of tutorials that present prefered Tang design patterns.  By structuring your application according to the patterns we suggest throughout the tutorials, you will allow our static analysis framework, Tint ("Tang Lint"), to detect problematic design patterns and high-level configuration problems as part of your build.  These patterns provide the cornerstone for a number of more advanced features, such as interacting with legacy configuration systems, designing for cross-language applications, and multi-tenancy issues, such as secure injections of untrusted application code.  To the best of our knowledge, implementing such tools and addressing these real-world implementation constraints would be difficult, or even impossible, atop competing frameworks.

Design principles
=================

Tang encourages application developers to specify default implementations and constructor parameters in terms of code annotations and configuration modules.  This avoids the need for a number of subtle (and often confusing) dependency injection software patterns, though it does lead to a different approach to dependency injection than other frameworks encourage.

In the process of building complicated systems built atop Tang, we found that, as the length of configurations that are passed around at runtime increased, it rapidly became impossible to debug or maintain our higher-level applications.  In an attempt to address this problem, traditional dependency injection systems actually compound this issue.  They encourage the developers of each application-level component to implement hand-written "Modules" that are executed at runtime.  Hand-written modules introspect on the current runtime configuration, augment and modify it, and then return a new configuration that takes the new application component into account.

In other systems, developers interact with modules by invoking ad-hoc builder methods, and passing configurations (in the correct order) from module to module.  Modules frequently delgate to each other, either via inheritance or wrappers.  This makes it difficult for developers and end-users to figure out which value of a given parameter will be used, or even to figure out why it was (or was not) set.

Tang provides an alternative called `ConfigurationModule`s:

- `Configurations` and `ConfigurationModules` are "just data," and can be read and written in human readable formats.
- Interfaces and configuration parameters are encouraged to specify defaults, significantly shortening the configurations generated at runtime, and making it easy to see what was "strange" about a given run of the application.
- Tang's static analysis and documentation tools sanity check `ConfigurationModule`s, and document their behavior and any extra parameters they export.
- Configuration options can be set at most once.  This avoids (or at least detects) situations in which users and application-level code inadvertantly "fight" over the setting of a particular option.

The last property comes from Tang's use of _monotonic_ set oriented primitives.  This allows us to leverage recent theoretical results in commtative data types; particularly CRDTs, and the CALM theorem.  Concretely:
- A large subset of Tang's public API is commutative, which frees application-level configuration and bootstrapping logic from worrying about the order in which configuration sources are processed at runtime.
- Tang can detect configuration and injection problems much earlier than is possible with other approaches.  Also, upon detecting a conflict, Tang lists the configuration sources that contributed to the problem.

Finally, Tang is divided into a set of "core" primtives, and higher-level configuration "formats".  Tang's core focuses on dependency injection and static checking of configurations.  The formats provide higher-level configuration languages primitives, such as distributed, cross-language injection, configuration files, and `ConfigurationModule`.  Each Tang format imports and/or exports standard Tang `Configuration` objects, which can then be composed with other configuration data at runtime.

Improvements to these formats are planned, such as command-line tab completion, and improved APIs for extremely complex applications that are built by composing multiple Tang configurations to inject arbitrary object graphs.
Furthermore, Tang formats include documentation facilities, and automatic command line and configuration file parsing.  From an end-user perspective, this takes a lot of the guesswork out of configuration file formats.

Although Tang surfaces a text-based interface for end-users of the applications built atop it, all configuration options and their types are specified in terms of Java classes and annotations.  As with the core Tang primitives, this allows the Java compiler to statically check Tang formats for problems such as inconsistent usage of configuration parameters, naming conflicts and so on.  This eliminates broad classes of runtime errors.   These checks can be run independently of the application's runtime environment, and can find problems both in the Java-level implementation of the system, and with user-provided configuration files.  The tools that perform these checks are designed to run as a post-processing step of projects built atop Tang.  Like the Java compiler checks, this prevents such errors from making it to production environments.  It also prevents such errors from being exposed to application logic or end-users, greatly simplifying applications built atop Tang.

Taken together, these properties greatly simplify dependency injection in distributed environments.  We expect Tang to be used in environments that are dominated by "plugin"-style APIs with many alternative implementations.  Tang cleanly separates concerns over configuration management, dependency injection and object implementations, which hides most of the complexity of dependency injection from plugin implementers.  It also prevents plugin implementations from inadvertently conflicting with each other or their runtime environements.  Such clean semantics are crucial in distributed, heterogeneous environments.

Tutorial: Getting started
=========================

This tutorial is geared toward people that would like to quickly get started with Tang, or that are modifying an existing Tang application.

Constructors, @Inject and @Parameter
------------------------

Suppose you are implementing a new class, and would like to automatically pass configuration parameters to it at runtime:

```java
package com.example;

public class Timer {
  private final int seconds;

  public Timer(int seconds) {
    if(seconds < 0) {
      throw new IllegalArgumentException("Cannot sleep for negative time!");
    }
    this.seconds = seconds;
  }

  public void sleep() throws Exception {
    java.lang.Thread.sleep(seconds * 1000);
  }
}
```
Tang encourages applications to use Plain Old Java Objects (POJOs), and emphasizes the use of immutable state for configuration parameters.  This reduces boiler plate (there is no need for extra setter methods), and does not interfere with encapsulation (the fields and even the constructor can be private).  Furthermore, it is trivial for well-written classes to ensure that all objects are completely and properly instantiated:  They simply need to check constructor parameters as any other POJO would, except that Tang never passes `null` references into constructors, allowing their implementations to assume that all parameter values are non-null.

Tang aims to provide end users with error messages as early as possible, and encourages developers to throw exceptions inside of constructors.  This allows it to automatically provide additional information to end-users when things go wrong:

```
Exception in thread "main" com.microsoft.tang.exceptions.InjectionException: Could not invoke constructor: new Timer(Integer Seconds = -1)
	at com.microsoft.tang.implementation.java.InjectorImpl.injectFromPlan(InjectorImpl.java:585)
	at com.microsoft.tang.implementation.java.InjectorImpl.getInstance(InjectorImpl.java:449)
	at com.microsoft.tang.implementation.java.InjectorImpl.getInstance(InjectorImpl.java:466)
	at com.microsoft.tang.examples.Timer.main(Timer.java:48)
Caused by: java.lang.IllegalArgumentException: Cannot sleep for negative time!
	at com.microsoft.tang.examples.Timer.<init>(Timer.java:25)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
	at sun.reflect.NativeConstructorAccessorImpl.newInstance(Unknown Source)
	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(Unknown Source)
	at java.lang.reflect.Constructor.newInstance(Unknown Source)
	at com.microsoft.tang.implementation.java.InjectorImpl.injectFromPlan(InjectorImpl.java:569)
	... 3 more
```

In order for Tang to instantiate an object, we need to annotate the constructor with an `@Inject` annotation.  While we're at it, we'll define a configuration parameter, allowing us to specify seconds on the command line and in a config file:

```java
package com.example;

import javax.inject.Inject;

import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.annotations.Parameter;

public class Timer {
  @NamedParameter(default_value="10",
      doc="Number of seconds to sleep", short_name="sec")
  class Seconds implements Name<Integer> {}
  private final int seconds;

  @Inject
  public Timer(@Parameter(Seconds.class) int seconds) {
    if(seconds < 0) {
      throw new IllegalArgumentException("Cannot sleep for negative time!");
    }
    this.seconds = seconds;
  }

  public void sleep() throws Exception {
    java.lang.Thread.sleep(seconds * 1000);
  }
}
```
A few things happened here.  First, we create the new configuration parameter by declaring a dummy class that implements Tang's `Name` interface.  `Name` is a generic type with a single mandatory parameter that specifies the type of object to be passed in.  Since `Seconds` implements `Name<Integer>`, it is a parameter called `Seconds` that expects `Integer` values.  More precisely, `Seconds` is actually named `com.example.Timer.Seconds`.  This reliance on language types to define parameter names exposes parameters to the compiler and IDE.  Concretely:
 * `javac` maps from `Seconds` to the full class name in the usual way, preventing parameters with the same name, but in different packages from conflicting.
 * The Java classloader ensures that classes are unique at runtime. 
 * Standard IDE features, such as code navigation, completion and refactoring work as they normally would for class names.


All instances of `Name` must be annotated with `@NamedParameter`, which takes the following optional parameters:
 * `default_value`: The default value of the constructor parameter, encoded as a string.  Tang will parse this value (and ones in config files and on the command line), and pass it into the constructor.  For convenience Tang includes a number of helper variants of default value.  `default_class` takes a Class (instead of a String), while `default_values` and `default_classes` take sets of values.
 * `short_name`: The name of the command line option associated with this parameter.  If omitted, no command line option will be created.  Short names must be registered by calling `registerShortName()` on the instance of `com.microsoft.tang.formats.CommandLine` that will process the command line options.
 * `doc` (optional): Human readable documentation that describes the purpose of the parameter.

Tang only invokes constructors that have been annotated with `@Inject`.  This allows injectable constructors to coexist with ones that should not be invoked via dependency injection (such as ones with destructive side effects, or that expect `null` references).  Constructor parameters must not be ambiguous.  If two parameters in the same constructor have the same type, then they must be annotated with `@Parameter`, which associates a named parameter with the argument.  Furthermore, two parameters to the same constructor cannot have the same name.  This allows Tang to safely invoke constructors without exposing low level details (such as parameter ordering) as configuration options.

Example:

![screenshot of tooltip](doc/tooltip.png "IDE contextual help contains information about Tang named parameters")

Configuration modules
---------
Configuration modules allow applications to perform most configuration generation and verification tasks at build time.  This allows Tang to automatically generate rich configuration-related documentation, to detect problematic design patterns, and to report errors before the application even begins to run.

In the example below, we extend the Timer API to include a second implementation that simply outputs the amount of
time a real timer would have slept to stderr.  In a real unit testing example, it would likely interact with a scheduler based on logical time.  Of course, in isolation, having the ability to specify configuration parameters is not particularly useful; this example also adds a `main()` method that invokes Tang, and instantiates an object.

The process of instantiting an object with Tang is called _injection_.  As with configurations, Tang's injection process is designed to catch as many potential runtime errors as possible before application code begins to run.  This simplifies debugging and eliminates many types of runtime error handling code, since many configurations can be caught before running (or examining) application-specific initialization code. 


```java
package com.microsoft.tang.examples.timer;

import javax.inject.Inject;

import com.microsoft.tang.Configuration;
import com.microsoft.tang.Tang;

import com.microsoft.tang.annotations.DefaultImplementation;
import com.microsoft.tang.annotations.Name;
import com.microsoft.tang.annotations.NamedParameter;
import com.microsoft.tang.annotations.Parameter;

import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;

import com.microsoft.tang.formats.ConfigurationModule;
import com.microsoft.tang.formats.ConfigurationModuleBuilder;
import com.microsoft.tang.formats.OptionalParameter;

@DefaultImplementation(TimerImpl.class)
public interface Timer {
  @NamedParameter(default_value="10",
      doc="Number of seconds to sleep", short_name="sec")
  public static class Seconds implements Name<Integer> { }
  public void sleep() throws Exception;
}

public class TimerImpl implements Timer {

  private final int seconds;
  @Inject
  public TimerImpl(@Parameter(Timer.Seconds.class) int seconds) {
    if(seconds < 0) {
      throw new IllegalArgumentException("Cannot sleep for negative time!");
    }
    this.seconds = seconds;
  }
  @Override
  public void sleep() throws Exception {
    java.lang.Thread.sleep(seconds);
  }

}

public class TimerMock implements Timer {

  public static class TimerMockConf extends ConfigurationModuleBuilder {
    public static final OptionalParameter<Integer> MOCK_SLEEP_TIME = new OptionalParameter<>();
  }
  public static final ConfigurationModule CONF = new TimerMockConf()
    .bindImplementation(Timer.class, TimerMock.class)
    .bindNamedParameter(Timer.Seconds.class, TimerMockConf.MOCK_SLEEP_TIME)
    .build();
  
  private final int seconds;
  
  @Inject
  TimerMock(@Parameter(Timer.Seconds.class) int seconds) {
    if(seconds < 0) {
      throw new IllegalArgumentException("Cannot sleep for negative time!");
    }
    this.seconds = seconds; 
  }
  @Override
  public void sleep() {
    System.out.println("Would have slept for " + seconds + "sec.");
  }

  public static void main(String[] args) throws BindException, InjectionException, Exception {
    Configuration c = TimerMock.CONF
      .set(TimerMockConf.MOCK_SLEEP_TIME, 1)
      .build();
    Timer t = Tang.Factory.getTang().newInjector(c).getInstance(Timer.class);
    System.out.println("Tick...");
    t.sleep();
    System.out.println("...tock.");
  }
}
```
Again, there are a few things going on here:
   - First, we push the implementation of `Timer` into a new class, `TimerImpl`.  The `@DefaultImplementation` tells Tang to use `TimerImpl` when no other implementation is explicitly provided.
   - We leave the `Sleep` class in the Timer interface.  This, plus the `@DefaultImplementation` annotation maintain backward compatibility with code that used Tang to inject the old `Timer` class.
   - The `TimerMock` class includes a dummy implementation of Timer, along with a `ConfigurationModule` final static field called `CONF`.
   - The main method uses `CONF` to generate a configuration.  Rather than set `Timer.Sleep` directly, it sets `MOCK_SLEEP_TIME`.  In a more complicated example, this would allow `CONF` to route the sleep time to testing infrastructure, or other classes that are specific to the testing environment or implemenation of `TimerMock`.

`ConfigurationModule`s serve a number of purposes:
   - They allow application and library developers to encapsulate the details surrounding their code's instantiation.
   - They provide Java APIs that expose `OptionalParameter`, `RequiredParameter`, `OptionalImplementation`, `RequiredImpementation` fields.  These fields tell users of the ConfigurationModule which subsystems of the application require which configuration parameters, and allow the author of the ConfigurationModule to use JavaDoc to document the parameters they export.
   - Finally, because ConfigurationModule data structures are populated at class load time (before the application begins to run), they can be inspected by Tang's static analysis tools.

These tools are provided by `com.microsoft.tang.util.Tint`, which is included by default in all Tang builds.  As long as Tang is on the classpath, invoking:
```
java com.microsoft.tang.util.Tint --doc tangdoc.html
```
will perform full static analysis of all classes the class path, and emit a nicely formatted HTML document.  The documentation generated by Tint includes cross-references between configuration options, interfaces, classes, and the `ConfigurationModules` that use and set them. 

Here is the documentation for the Timer example:

![screenshot of tooltip](doc/tangdoc.png "Automatically generated documentation of Tang configuration data.")

Note that this documentation is generated before the application code runs, so it does not reflect the fact that `MOCK_SLEEP_TIME` is set to 1 by `main()`.

Here are some sample Tint errors.  These (and others) can be run by passing `--tang-tests` into Tint, and ensuring that Tang's unit tests are on the class path.:
```
interface com.microsoft.tang.MyEventHandlerIface declares its default implementation to be non-subclass class com.microsoft.tang.MyEventHandler
class com.microsoft.tang.WaterBottleName defines a default class com.microsoft.tang.GasCan with a type that does not extend its target's type com.microsoft.tang.Bottle<com.microsoft.tang.Water>
Named parameters com.microsoft.tang.examples.Timer$Seconds and com.microsoft.tang.examples.TimerV1$Seconds have the same short name: sec
Named parameter com.microsoft.tang.implementation.AnnotatedNameMultipleInterfaces implements multiple interfaces.  It is only allowed to implement Name<T>
Found illegal @NamedParameter com.microsoft.tang.implementation.AnnotatedNotName does not implement Name<?>
interface com.microsoft.tang.implementation.BadIfaceDefault declares its default implementation to be non-subclass class java.lang.String
class com.microsoft.tang.implementation.BadName defines a default class java.lang.Integer with a raw type that does not extend of its target's raw type class java.lang.String
Named parameter com.microsoft.tang.implementation.BadParsableDefaultClass defines default implementation for parsable type java.lang.String
Class com.microsoft.tang.implementation.DanglingUnit has an @Unit annotation, but no non-static inner classes.  Such @Unit annotations would have no effect, and are therefore disallowed.
Cannot @Inject non-static member class unless the enclosing class an @Unit.  Nested class is:com.microsoft.tang.implementation.InjectNonStaticLocalType$NonStaticLocal
Named parameter com.microsoft.tang.implementation.NameWithConstructor has a constructor.  Named parameters must not declare any constructors.
Named parameter type mismatch.  Constructor expects a java.lang.String but Foo is a java.lang.Integer
public com.microsoft.tang.implementation.NonInjectableParam(int) is not injectable, but it has an @Parameter annotation.
Detected explicit constructor in class enclosed in @Unit com.microsoft.tang.implementation.OuterUnitBad$InA  Such constructors are disallowed.
Repeated constructor parameter detected.  Cannot inject constructor com.microsoft.tang.implementation.RepeatConstructorArg(int,int)
Named parameters com.microsoft.tang.implementation.ShortNameFooA and com.microsoft.tang.implementation.ShortNameFooB have the same short name: foo
Named parameter com.microsoft.tang.implementation.UnannotatedName is missing its @NamedParameter annotation.
Field com.microsoft.tang.formats.MyMissingBindConfigurationModule.BAD_CONF: Found declared options that were not used in binds: { FOO_NESS }
```

Injecting objects with `getInstance()`
--------------------------------------

Above, we explain how to register constructors with Tang, and how to configure Tang to inject the desired objects at runtime.  This section explains how Tang actually instantiates objects, and how the primitives it provides can be combined to support sophisticated application architectures.

In order to instantiate objects with Tang, one must invoke Tang.Factory.getTang().newInjector(Configuration...).  This returns a new "empty" injector that will honor the configuration options that were set in the provided configurations, and that will have access to a merged version of the classpath they refer to.

In a given Tang injector, all classes are treated as singletons: at most one instance of each class may exist.  Furthermore, Tang Configuration objects are designed to be built up from trees of related (but non-conflicting) configuration files, command line parameters, and so on.  At first, this may seem to be overly restrictive, since it prevents applications from creating multiple instances of the same class (or even two classes that require different values of the same named parameter).

Tang addresses this by providing the runtime environment more explicit control over object and configuration parameter scopes.  Taken together, `forkInjector()`, `bindVolatile()` and `InjectionFuture<T>` allow Tang to inject arbitrary sets of objects (including ones with multiple instances of the same class).

Other injection frameworks take a different approach, and allow class implementations to decide if they should be singletons across a given JVM (e.g., with an `@Singleton` annotation), user session (for web services), user connection, and so on.  This approach has at least two problems:
 * It is not general purpose: after all, it encodes deployment scenarios into the injection framework and application APIs!
 * It trades one barrier to composability and reuse: _hard-coded constructor invocations_ with another: _hard-coded runtime environments_.  The former prevents runtime environments from adapting to application-level changes, while the latter prevents application code from adapting to new runtimes.

Tang's approach avoids both issues by giving the implementation of the runtime environment explicit control over object scopes and lifetimes.

`forkInjector()` makes a copy of a given injector, including references to all the objects already instantiated by the original injector.  This allows runtime environments to implement scopes.  First, a root injector is created.  In order to create a child scope, the runtime simply invokes `forkInjector()` on the root context, and optionally passes additional `Configuration` objects in.  These additional configurations allow the runtime to specialize the root context.

Although the forked injector will have access to any objects and configuration bindings that existed when `forkInjector()` was called, neither the original nor the forked injectors will reflect future changes to the other injector.

The second primitive, `bindVolatile()`, provides an injector with an instance of a class or named parameter.  The injector treats this instance as though it had injected the object directly.  This:
 * allows passing of information between child scopes
 * makes it possible to create (for example) chains of objects of the same type
 * and allows objects that cannot be instantiated via Tang to be passed into injectable constructors.

### Cyclic injections

Although the above primitives allow applications to inject arbitrary DAGs (directed acyclic graphs) of objects, they do not support cycles of objects.  Tang provides the `InjectionFuture<T>` interfaces to support such _cyclic injections_.

When Tang encounters a constructor parameter of type `InjectionFuture<T>`, it injects an object that provides a method `T get()` that returns an injected instance of `T`.  


This can be used to break cycles:

```java
A(B b) {...}
B(InjectionFuture<A> a) {...}
```

In order to inject an instance of `A`, Tang first injects an instance of `B` by passing it an `InjectionFuture<A>`.  Tang then invoke's `A`'s constructor, passing in the instance of `B`.  Once the constructor returns, the new instance of `A` is passed into `B`'s `InjectionFuture<A>`.  At this point, it becomes safe for `B` to invoke `get()`, which establishes the circular reference.

Therefore, along with `forkInjector()` and `bindVolatile()`, this allows Tang to inject arbitrary graphs of objects.  This pattern avoids non-final fields (once set, all fields of all objects are constant), and it also avoids boiler plate error handling code that checks to see if `B`'s instance of `A` has been set.


When `get()` is called after the application-level call to `getInstance()` returns, it is guranteed to return a non-null reference to an injected instance of the object.  However, if `get()` is called _before_ the constructor it was passed to returns, then it is guaranteed to throw an exception.    In between these two points in time, `get()`'s behavior is undefined, but, for the sake of race-detection and forward compatibility it makes a best-effort attempt to throw an exception.

Following Tang's singleton semantics, the instance returned by `get()` will be the same instance the injector would pass into other constructors or return from `getInstance()`.

Alternative configuration sources
=================================

Tang provides a number of so-called _formats_ that interface with external configuration data.  `ConfigurationModule` is one such example (see above).  These formats transform configuration data to and from Tang's raw configuration API.  The raw API provides an implementation of ConfigurationBuilder, which implements most of Tang's configuration checks.  It also provides a `JavaConfigurationBuilder` interface provides convenience methods that take Java Classes, and leverage Java's generic type system to push a range of static type checks to Java compilation time.

Raw configuration API
---------
Tang also provides a lower level configurtion API for applications that need more dynamic control over their configurations:

```java
...
import com.microsoft.tang.Tang;
import com.microsoft.tang.ConfigurationBuilder;
import com.microsoft.tang.Configuration;
import com.microsoft.tang.Injector;
import com.microsoft.tang.exceptions.BindException;
import com.microsoft.tang.exceptions.InjectionException;

...
  public static void main(String[] args) throws BindException, InjectionException {
    Tang tang = Tang.Factory.getTang();
    ConfigurationBuilder cb = (ConfigurationBuilder)tang.newConfigurationBuilder();
    cb.bindNamedParameter(Timer.Seconds.class, 5);
    Configuration conf = cb.build();
    Injector injector = tang.newInjector(conf);
    if(!injector.isInjectable(Timer.class)) {
      System.err.println("If isInjectable returns false, the next line will throw an exception");
    }
    Timer timer = injector.getInstance(Timer.class);

    try {
      System.out.println("Tick...");
      timer.sleep();
      System.out.println("Tock.");
    } catch(InterruptedException e) {
      e.printStackTrace();
    }
  }
```
The first step in using Tang is to get a handle to a Tang object by calling "Tang.Factory.getTang()".  Having obtained a handle, we run through each of the phases of a Tang injection:
   * We use `ConfigurationBuilder` objects to tell Tang about the class hierarchy that it will be using to inject objects and (in later examples) to register the contents of configuration files, override default configuration values, and to set default implementations of classes.  `ConfigurationBuilder` and `ConfigurationModuleBuider` export similar API's.  The difference is that `ConfigurationBuilder` produces `Configuration` objects directly, and is designed to be used at runtime.  `ConfigurationModuleBuilder` is desgined to produce data structures that will be generated and analyzed during the build, and at class load time.
   * `bindNamedParameter()` overrides the default value of Timer.Sleep, setting it to 5.  Tang inteprets the 5 as a string, but allows instances of Number to be passed in as syntactic sugar.
   * We call `.build()` on the `ConfigurationBuilder`, creating an immutable `Configuration` object.  At this point, Tang ensures that all of the classes it has encountered so far are consistent with each other, and that they are suitable for injection.  When Tang encounters conflicting classes or configuration files, it throws a `BindException` to indicate that the problem is due to configuration issues. Note that `ConfigurationBuilder` and `Configuration` do not determine whether or not a particular injection will succeed; that is the business of the _Injector_.
   * To obtain an instance of Injector, we pass our Configuration object into `tang.newInjector()`.
   * `injector.isInjectable(Timer.class)` checks to see if Timer is injectable without actually performing an injection or running application code.  (Note that, in this example, the Java classloader may have run application code.  For more information, see the advanced tutorials on cross-language injections and securely building configurations for untrusted code.)
   * Finally, we call `injector.getInstance(Timer.class)`.  Internally, this method considers all possible injection plans for `Timer`.  If there is exactly one such plan, it performs the injection.  Otherwise, it throws an `InjectionException`.



Tang configuration information can be divided into two categories.  The first type, _parameters_, pass values such as strings and integers into constructors.  Users of Tang encode configuration parameters as strings, allowing them to be stored in configuration files, and passed in on the command line.

The second type of configuration option, _implementation bindings_, are used to tell Tang which implementation should be used when an instance of an interface is requested.  Like configuration parameters, implementation bindings are expressible as strings: Tang configuration files simply contain the raw (without the generic parameters) name of the Java Classes to be bound together.

New parameters are created and passed into constructors as in the examples above, by creating implementations of `Name<T>`, and adding `@NamedParameter`, `@Parameter` and `@Inject` annotations as necessary.  Specifying implementations for interfaces is a bit more involved, as a number of subtle use cases arise.

However, all configuration settings in Tang can be unambiguously represented as a `key=value` pair that can be interpreted either asan `interface=implementation` pair or a `configuration_parameter=value` pair.  This maps well to Java-style properties files.  For example:

```properties
com.examples.Interface=com.examples.Implementation
```

tells Tang to create a new Implementation each time it wants to invoke a constructor that asks for an instance of Interface.  In most circumstances, Implementation extends or implements Interface (`ExternalConstructors` are the exception -- see the next section).  In such cases, Tang makes sure that Implementation contains at least one constructor with an `@Inject` annotation, and performs the binding.

See the `ConfigurationFile` API for more information about processing configuration files in this format.


<!--
[**TODO:** document configuration file API here]

[**TODO:** explain `processCommandLine()`]


Advanced Object Injection Patterns
----------------------------------

### Cyclic injections with `InjectionFuture`

Unlike other dependency injection frameworks, all Tang injections are performed by invoking object constructors.  Although it is generally considered bad practice to create classes with circular dependencies, there are valid reasons to have two objects that point to each other at runtime.  For concreteness, call these two objects `a` and `b`.

The simplest way to achieve this is to add a setter to one of the objects, and then invoke the setter in the other object's constructor:

```java
  a.setB(b);
```

Although this works, it has a few disadvantages:
 - `a` is no longer immutable (or fully instantiated when its constructor returns), which means that its implementation should contain checks to ensure that b is set exactly once before execution proceeds.
 - `a`'s dependency on `b` is no longer apparent from `a`'s constructor signatures.

This is a shame; when coupled with immutable fields, Tang's use of constructor-based injetion eliminates both these problems, saving application developers from implementing and documenting setters and runtime checks.

Based on these observations, Tang offers an alternative called an `InjectionFuture`.  In our example, we would declare two consructors:
```java
@Inject A(InjectionFuture<B> b) { this.b_f = b; }
@Inject B(A a ) { this.a = a; }
```
Instead of passing `b` directly into `a`'s constructor, Tang creates an `InjectionFuture<B>` with a single method `public B get()`.  `get()` is guaranteed to throw an exception if called before `A`'s constructor returns, and is guaranteed to return the injected instance of `B` if called after Tang's `Injector.getInstance()` successfuly returns.  This allows application designers to break cycles in injections without resorting to mutable fields, or runtime error checking.  `get()` throws a `RuntimeException` that object implementors should ignore and pass up the stack.  Note that calling methods that transitively call `InjectionFuture.get()` within a constructor is fundamentally unsafe.  Such calls have undefined behavior, but Tang makes a best effort attempt to throw an exception when such a call is made.


### Using `@Unit` to implement multiple versions of a generic interface

### Using `ExternalConstructor` to inject legacy code or delegate to a subtype during injection.

TODO promote to top-level section.

Tang's _ExternalConstructor_ API supports injection of legacy code.  If Implementation does not subclass Interface, Tang checks to see if it subclasses ExternalConstructor<? extends Interface> instead.  If so, Tang checks that Implementation has an `@Inject` annotation on at least one constructor, and performs the binding as usual.  At injection time, Tang injects Implementation as though it implemented Interface, and then calls `newInstance()`, which returns the value to be injected.  Note that `ExternalConstructor` objects are single-use: `newInstance()` will only be called once.  If the `ExternalConstructor` class is marked as a singleton, Tang internally retains the return value of `newInstance()`, exactly as if the object had been created with a regular constructor.

Tutorial: Complex application architectures
===========================================

Composing configurations from multiple sources
----------------------------------------------

Distributed dependency injection
--------------------------------
In Tang, distributed injection is performed by writing Tang's current state out to configuration files, shipping them to remote machines, and using the configuration file to instantiate an identical Tang instance on the remote machine.  Two methods support such use cases.  The first is part of the Configuration API, and writes a well-formed configuration file to an output stream.  Its method signature is self-explanatory:

```java
public void writeConfigurationFile(OutputStream s)
```

Reading the file back is the responsibility of `ConfigurationBuilder`.  The following methods read the file line by line, merging the Configuration options they find with the current state of the `ConfigurationBuilder`.  If a conflicting or already-set option is encountered, processing halts on the line that caused the problem, and a `BindException` is thrown:

```java
public void addConfiguration(final File istream) throws IOException, BindException;
public void addConfiguration(final String istream) throws BindException;
```

Bind
----
Sometimes it is necessary to compute configuration information at runtime, and pass the result into Tang.  Tang provides two types of `bind()` methods for such purposes.  The first reside in `configurationBuilder()`, and are designed to preserve Tang's ability to write back the resulting configuration to a file.  Like configuration files, these methods can tell Tang which implementation should be used for a given interface, provide strings to be parsed into configuration parameters, and so on:

```java
void bind(String iface, String impl) throws ClassNotFoundException;
void bind(Class<T> iface, Class<?> impl);
void bindImplementation(Class<T> iface, Class<? extends T> impl);
void bindSingletonImplementation(Class<T> iface, Class<? extends T> impl);
void bindSingleton(Class<T> iface) throws BindException;
void bindNamedParameter(Class<? extends Name<T>> name, String value);
void bindConstructor(Class<T> c, Class<? extends ExternalConstructor<? extends T>> v);
```
Each of these methods throws BindException as well as the exceptions mentioned above, and behaves identically to the analogous configuration file primitives discussed above.  Note that, when possible, adding final [`StaticConfiguration`](static-configuration) objects to class definitions objects is always preferable to writing a method that invokes bind...() directly.

The second class of bind operations allow callers to pass object instances to Tang directly.  This prevents it from writing back its current state to a configuration file.  Because these methods are incompatible with writing configuration files, their names contain the word "Volatile", and they are part of the Injector API instead of ConfigurationBuilder.  Injectors cannot be serialized, and they are not allowed to modify the Configuration object that was used to create them, making it impossible to use the Tang API to write volatile objects to disk.

```java
Injector bindVolatileInstance(Class<T> iface, T inst) throws BindException;
Injector bindVolatileParameter(Class<? extends Name<T>> iface, T inst) throws BindException;
```
Note that these methods return new Injector objects.  Tang Injectors are immutable, and the original Injector is not modified by these calls.

A final method, `getNamedParameter()`, is sometimes useful when dealing with instances of objects used for Tang injections.  Unlike `getInstance()`, which performs a normal injection, `getNamedParameter()` instantiates an object in the same way as it would during an injection, as it prepares to pass a configuration parameter to a constructor (note that whether a new instance of the parameter is instantiated for each constructor invocation is not specified by the Tang API, so while the object returned likely `.equals()` the one that would be passed to a constructor, it may or may not `==` it.

Roadmap
=======

Injection plans
---------------
[Coming soon]

Language interoperability
-------------------------
[Coming soon]

When things go wrong
--------------------
In the timer example, we specified a default value for the Sleep parameter.  If we hadn't done this, then the call
to `getInstance()` would have thrown an exception:
````
Exception in thread "main"
java.lang.IllegalArgumentException: Attempt to inject infeasible plan: com.example.Timer(Integer Seconds = null)
...
````
Since Tang refuses to inject null values into object constructors, the plan to invoke `Timer(null)` is considered infeasible.  Note that this error message enumerates all possible injection plans.  If Timer had more constructors or implementations those would be enumerated here as well.  Similarly, if more than one feasible plan existed, Tang would refuse to perform the injection, and throw a similar exception.

In both cases, the solution is to set additional configuration parameters to create a single feasible plan.  This can be done using any of the methods described above.
-->
Looking under the hood
----------------------

### InjectionPlan

InjectionPlan objects explain what Tang would do to instantiate a new object, but don't actually instantiate anything.
Add the following lines to the Timer example;

````java
import com.microsoft.tang.implementation.InjectionPlan;
import com.microsoft.tang.implementation.InjectorImpl;
...
    InjectorImpl injector = (InjectorImpl)tang.newInjector(conf);
    InjectionPlan<Timer> ip = injector.getInjectionPlan(Timer.class);
    System.out.println(ip.toPrettyString());
    System.out.println("Number of plans:" + ip.getNumAlternatives());
````

Running the program now produces a bit of additional output:
````
new Timer(Integer Seconds = 10)
Number of plans:1
````

InjectionPlan objects can be serialized to protocol buffers.  The following file documents their format:

https://github.com/Microsoft-CISL/TANG/blob/master/tang/src/main/proto/injection_plan.proto

### ClassHierachy

InjectionPlan explains what would happen if you asked Tang to take some action, but it doesn't provide much insight into Tang's view of the object hierarchy, parameter defaults and so on.  ClassHierarchy objects encode the state that Tang gets from .class files, including class inheritance relationships, parameter annotations, and so on.

Internally, in the example above, TypeHierarchy walks the class definition for Timer, looking for superclasses, interfaces, and classes referenced by its constructors.

ClassHierarchy objects can be serialized to protocol buffers.  The following file documents their format:

https://github.com/Microsoft-CISL/TANG/blob/master/tang/src/main/proto/class_hierarchy.proto

The java interfaces are available in this package:

https://github.com/Microsoft-CISL/TANG/tree/master/tang/src/main/java/com/microsoft/tang/types

## THIRD PARTY SOFTWARE
This software is built using Maven.  Maven allows you to obtain software libraries from other sources as part of the build process.  Those libraries are offered and distributed by third parties under their own license terms.  Microsoft is not developing, distributing or licensing those libraries to you, but instead, as a convenience, enables you to use this software to obtain those libraries directly from the creators or providers.  By using the software, you acknowledge and agree that you are obtaining the libraries directly from the third parties and under separate license terms, and that it is your responsibility to locate, understand and comply with any such license terms.  Microsoft grants you no license rights for third-party software or libraries that are obtained using this software.

The list of libraries pulled in this way includes, but is not limited to:

 * com.google.code.findbugs:jsr305:jar:1.3.9:compile
 * com.google.guava:guava:jar:11.0.2:compile
 * com.google.protobuf:protobuf-java:jar:2.5.0:compile
 * commons-cli:commons-cli:jar:1.2:compile
 * commons-configuration:commons-configuration:jar:1.9:compile
 * commons-lang:commons-lang:jar:2.6:compile
 * commons-logging:commons-logging:jar:1.1.1:compile
 * dom4j:dom4j:jar:1.6.1:compile
 * javax.inject:javax.inject:jar:1:compile
 * junit:junit:jar:4.10:test
 * org.hamcrest:hamcrest-core:jar:1.1:test
 * org.javassist:javassist:jar:3.16.1-GA:compile
 * org.mockito:mockito-core:jar:1.9.0:test
 * org.objenesis:objenesis:jar:1.0:test
 * org.reflections:reflections:jar:0.9.9-RC1:compile
 * xml-apis:xml-apis:jar:1.0.b2:compile

An up-to-date list of dependencies pulled in this way can be generated via `mvn dependency:list` on the command line.


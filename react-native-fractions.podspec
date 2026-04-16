require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-fractions"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.homepage     = "https://github.com/maderesponsively/react-native-fractions"
  s.license      = { :type => package["license"], :file => "LICENSE" }
  s.authors      = { "David Smith" => "hello@maderesponsively.com" }
  s.platforms    = { :ios => "13.4" }
  s.source       = {
    :git => "https://github.com/maderesponsively/react-native-fractions.git",
    :tag => "v#{s.version}"
  }
  s.source_files = "ios/**/*.{h,m,mm,swift}"
  s.requires_arc = true

  s.dependency "React-Core"
end

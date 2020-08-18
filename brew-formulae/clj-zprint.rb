class CljZprint < Formula
  desc "Formatter for Clojure"
  homepage "https://github.com/kkinnear/zprint"
  version "2020.08.09"

  if OS.linux?
    url "https://github.com/kkinnear/zprint/releases/download/1.0.0/zprintl-1.0.0"
    sha256 "b707f1188c175c2028c014f0ae88cb384283aa6d097bb31298d66852162581b1"
  else
    url "https://github.com/kkinnear/zprint/releases/download/1.0.0/zprintm-1.0.0"
    sha256 "b707f1188c175c2028c014f0ae88cb384283aa6d097bb31298d66852162581b1"
  end

  def install
    system "mv zprintm-1.0.0 clj-zprint"
    system "chmod 755 clj-zprint"
    bin.install "clj-zprint"
  end

  test do
    system "#{bin}/clj-zprint"
  end
end

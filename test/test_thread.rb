require 'test/unit'

class TestThread < Test::Unit::TestCase
  def test_running_and_finishing
    thread = Thread.new {
      $toto = 1
    }
    thread.join
    assert_equal(1, $toto)
    assert_equal(false, thread.status)
  end

  def test_local_variables
    v = nil
    t = Thread.new { v = 1 }
    t.join
    assert_equal(1, v)
  end

  def test_taking_arguments
    v = nil
    t = Thread.new(10) {|argument| v = argument }
    t.join
    assert_equal(10, v)
  end

  def test_thread_current
    t = Thread.current
    assert_equal(t, Thread.current)
  end

  def test_thread_local_variables
    v = nil
    t = Thread.new {
      Thread.current[:x] = 1234
      assert_equal(1234, Thread.current[:x])
      assert_equal(nil, Thread.current[:y])
      assert(Thread.current.key?(:x))
      assert(! Thread.current.key?(:y))
    }
    t.join
    assert(! Thread.current.key?(:x))
    Thread.current[:x] = 1
    assert_equal([:x], Thread.current.keys)
    Thread.current["y"] = 2
    Thread.current[3] = "three"
    assert(Thread.current.key?("y"))
    assert(Thread.current.key?(3))
  end

  def test_status
    v = nil
    t = Thread.new {
      v = Thread.current.status
    }
    t.join
    assert_equal("run", v)
    assert_equal(false, t.status)
  end

  def thread_foo()
    raise "hello"
  end
  def test_error_handling
    e = nil
    t = Thread.new {
      thread_foo()
    }
    begin
      t.join
    rescue RuntimeError => error
      e = error
    end
    assert(! e.nil?)
    assert_equal(nil, t.status)
  end

  def test_joining_itself
    e = nil
    begin
      Thread.current.join
    rescue ThreadError => error
      e = error
    end
    assert(! e.nil?)
  end

  def test_raise
    e = nil
    t = Thread.new {
      while true
        Thread.pass
      end
    }
    t.raise("Die")
    begin
      t.join
    rescue RuntimeError => error
      e = error
    end
    assert(e.kind_of?(RuntimeError))
    
    # test raising in a sleeping thread
    e = 1
    set = false
    begin
      t = Thread.new { e = 2; set = true; sleep(100); e = 3 }
      while !set
        sleep(1)
      end
      t.raise("Die")
    rescue; end
    
    assert_equal(2, e)
    assert_raise(RuntimeError) { t.value }
  end

  def test_thread_value
    assert_raise(ArgumentError) { Thread.new { }.value(100) }
    assert_equal(2, Thread.new { 2 }.value)
    assert_raise(RuntimeError) { Thread.new { raise "foo" }.value }
  end
end

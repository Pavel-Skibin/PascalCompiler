program SystemFunctionsTest;
var
    x, y, z: integer;
    d: double;
begin
    { Test Inc }
    x := Inc(10);
    writeln('Inc(10)=', x);

    { Test Dec }
    y := Dec(10);
    writeln('Dec(10)=', y);

    { Test Abs with negative }
    z := Abs(-42);
    writeln('Abs(-42)=', z);

    { Test Abs with positive }
    z := Abs(42);
    writeln('Abs(42)=', z);

    { Test Inc with variable }
    x := 5;
    y := Inc(x);
    writeln('Inc(5)=', y);

    { Test Dec with variable }
    x := 5;
    y := Dec(x);
    writeln('Dec(5)=', y);

    { Test nested }
    x := Inc(Inc(5));
    writeln('Inc(Inc(5))=', x);

    { Test Abs with double }
    d := Abs(-3.14);
    writeln('Abs(-3.14)=', d);
end.
